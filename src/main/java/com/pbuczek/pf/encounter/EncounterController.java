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
    @ResponseBody
    public Encounter createEncounter(@RequestBody EncounterDto encounterDto) {
        String username = userRepo.findById(encounterDto.getUserId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("user with id %d not found", encounterDto.getUserId()))).getUsername();

        adminOrSpecificUserCheck(username);
        return encounterRepo.save(new Encounter(encounterDto));
    }

    @DeleteMapping(value = "/{encounterId}")
    @ResponseBody
    public int deleteEncounter(@PathVariable Integer encounterId) {
        Optional<Encounter> optionalEncounter = encounterRepo.findById(encounterId);
        if (optionalEncounter.isEmpty()) {
            return 0;
        }

        adminOrSpecificUserCheck(optionalEncounter.get().getUserId());
        return encounterRepo.deleteEncounter(encounterId);
    }

    @GetMapping
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<Encounter> readAllEncounters() {
        return encounterRepo.findAll();
    }

    @GetMapping(value = "/{encounterId}")
    @ResponseBody
    public Encounter readEncounter(@PathVariable Integer encounterId) {
        Encounter encounter = encounterRepo.findById(encounterId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("encounter with id %d not found", encounterId)));

        adminOrSpecificUserCheck(encounter.getUserId());
        return encounter;
    }

    @GetMapping(value = "/by-userid/{userid}")
    @ResponseBody
    public List<Encounter> readEncountersByUserid(@PathVariable Integer userid) {
        adminOrSpecificUserCheck(userid);
        return encounterRepo.findByUserId(userid);
    }

    @GetMapping(value = "/by-username/{username}")
    @ResponseBody
    public List<Encounter> readEncountersByUsername(@PathVariable String username) {
        adminOrSpecificUserCheck(username);
        return encounterRepo.findByUserId(userRepo.getIdByUsername(username));
    }

    @PatchMapping(value = "/description/{encounterId}")
    @ResponseBody
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

        adminOrSpecificUserCheck(encounter.getUserId());
        return encounterRepo.save(encounter);
    }


    private void adminOrSpecificUserCheck(Integer userId) {
        adminOrSpecificUserCheck(userRepo.getUsernameById(userId));
    }

    private void adminOrSpecificUserCheck(String username) {
        if (!securityService.isContextAdminOrSpecificUsername(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not authorized for this resource");
        }
    }
}
