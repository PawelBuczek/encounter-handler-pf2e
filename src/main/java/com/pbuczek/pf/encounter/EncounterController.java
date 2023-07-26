package com.pbuczek.pf.encounter;

import com.pbuczek.pf.security.SecurityHelper;
import com.pbuczek.pf.user.PaymentPlan;
import com.pbuczek.pf.user.User;
import com.pbuczek.pf.user.UserRepository;
import com.pbuczek.pf.user.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping(value = "/encounter")
public class EncounterController {

    private final static Map<PaymentPlan, Integer> ENCOUNTER_LIMITS = Map.of(
            PaymentPlan.FREE, 30,
            PaymentPlan.ADVENTURER, 100,
            PaymentPlan.HERO, 1000);

    EncounterRepository encounterRepo;
    UserRepository userRepo;
    SecurityHelper securityHelper;

    @Autowired
    public EncounterController(EncounterRepository encounterRepo, UserRepository userRepo, SecurityHelper securityHelper) {
        this.encounterRepo = encounterRepo;
        this.userRepo = userRepo;
        this.securityHelper = securityHelper;
    }

    @PostMapping
    public Encounter createEncounter(@RequestBody EncounterDto encounterDto) {
        User user = userRepo.findById(encounterDto.getUserId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("user with id %d not found", encounterDto.getUserId())));

        adminOrSpecificUserId(user.getId());

        Integer limit = ENCOUNTER_LIMITS.get(user.getPaymentPlan());
        if(!user.getType().equals(UserType.ADMIN) && encounterRepo.getCountOfEncountersByUserId(user.getId()) >= limit) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Cannot create. Reached limit of Encounters: %d", limit));
        }

        return encounterRepo.save(new Encounter(encounterDto));
    }

    @DeleteMapping(value = "/{encounterId}")
    public int deleteEncounter(@PathVariable Integer encounterId) {
        Optional<Encounter> optionalEncounter = encounterRepo.findById(encounterId);
        if (optionalEncounter.isEmpty()) {
            return 0;
        }

        adminOrSpecificUserId(optionalEncounter.get().getUserId());
        return encounterRepo.deleteEncounter(encounterId);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<Encounter> readAllEncounters() {
        return encounterRepo.findAll();
    }

    @GetMapping(value = "/{encounterId}")
    public Encounter readEncounter(@PathVariable Integer encounterId) {
        Encounter encounter = encounterRepo.findById(encounterId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("encounter with id %d not found", encounterId)));

        adminOrSpecificUserId(encounter.getUserId());
        return encounter;
    }

    @GetMapping(value = "/by-userid/{userid}")
    public List<Encounter> readEncountersByUserid(@PathVariable Integer userid) {
        adminOrSpecificUserId(userid);
        return encounterRepo.findByUserId(userid);
    }

    @GetMapping(value = "/by-username/{username}")
    public List<Encounter> readEncountersByUsername(@PathVariable String username) {
        adminOrSpecificUserId(userRepo.getIdByUsername(username));
        return encounterRepo.findByUserId(userRepo.getIdByUsername(username));
    }

    @PatchMapping(value = "/description/{encounterId}")
    public Encounter updateDescription(@PathVariable Integer encounterId, @RequestBody String description) {
        Encounter encounter = encounterRepo.findById(encounterId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("encounter with id %d not found", encounterId)));

        adminOrSpecificUserId(encounter.getUserId());

        description = description.trim();
        if (description.length() > Encounter.MAX_DESCRIPTION_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("description too long. Max '%d' signs allowed.", Encounter.MAX_DESCRIPTION_LENGTH));
        }

        encounter.setDescription(description);
        return encounterRepo.save(encounter);
    }

    @PatchMapping(value = "/published/{encounterId}")
    public Encounter changePublished(@PathVariable Integer encounterId) {
        Encounter encounter = encounterRepo.findById(encounterId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("encounter with id %d not found", encounterId)));

        adminOrSpecificUserId(encounter.getUserId());

        encounter.setPublished(!encounter.getPublished());
        return encounterRepo.save(encounter);
    }

    private void adminOrSpecificUserId(Integer userId) {
        if (!securityHelper.isContextAdminOrSpecificUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not authorized for this resource");
        }
    }
}
