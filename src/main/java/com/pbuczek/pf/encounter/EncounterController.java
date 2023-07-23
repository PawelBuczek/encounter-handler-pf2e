package com.pbuczek.pf.encounter;

import com.pbuczek.pf.security.SecurityService;
import com.pbuczek.pf.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping(value = "/encounter")
public class EncounterController {

    EncounterRepository encounterRepo;
    UserRepository userRepo;
    SecurityService securityService;

    @Autowired
    public EncounterController(EncounterRepository encounterRepo, UserRepository userRepo, SecurityService securityService) {
        this.encounterRepo = encounterRepo;
        this.userRepo = userRepo;
        this.securityService = securityService;
    }

    @PostMapping
    public Encounter createEncounter(@RequestBody EncounterDto encounterDto) {
        userRepo.findById(encounterDto.getUserId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("user with id %d not found", encounterDto.getUserId())));

        adminOrSpecificUserId(encounterDto.getUserId());
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
        description = description.trim();

        try {
            encounter.setDescription(description);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("cannot set description '%s' for encounter with id %d", description, encounterId));
        }

        adminOrSpecificUserId(encounter.getUserId());
        return encounterRepo.save(encounter);
    }

    @PatchMapping(value = "/published/{encounterId}")
    public Encounter changePublished(@PathVariable Integer encounterId) {
        Encounter encounter = encounterRepo.findById(encounterId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("encounter with id %d not found", encounterId)));

        try {
            encounter.setPublished(!encounter.getPublished());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("cannot publish/unpublish encounter with id %d", encounterId));
        }

        adminOrSpecificUserId(encounter.getUserId());
        return encounterRepo.save(encounter);
    }

    private void adminOrSpecificUserId(Integer userId) {
        if (!securityService.isContextAdminOrSpecificUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not authorized for this resource");
        }
    }
}
