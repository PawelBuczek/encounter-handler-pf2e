package com.pbuczek.pf.encounters.controller;

import com.pbuczek.pf.encounters.Encounter;
import com.pbuczek.pf.encounters.dto.EncounterDto;
import com.pbuczek.pf.encounters.repository.EncounterRepository;
import com.pbuczek.pf.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @Autowired
    public EncounterController(EncounterRepository encounterRepo, UserRepository userRepo) {
        this.encounterRepo = encounterRepo;
        this.userRepo = userRepo;
    }

    @PostMapping
    @ResponseBody
    public Encounter createEncounter(@RequestBody EncounterDto encounterDto) {
        userRepo.findById(encounterDto.getUserId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("user with id %d not found", encounterDto.getUserId())));

        return encounterRepo.save(new Encounter(encounterDto));
    }

    @GetMapping()
    @ResponseBody
    public List<Encounter> readAllEncounters() {
        return encounterRepo.findAll();
    }

    @GetMapping(value = "/by-user/{userId}")
    @ResponseBody
    public List<Encounter> readEncountersForUser(@PathVariable Integer userId) {
        return encounterRepo.findByUserId(userId);
    }

    @GetMapping(value = "/{encounterId}")
    @ResponseBody
    public Encounter readEncounter(@PathVariable Integer encounterId) {
        Optional<Encounter> optionalEncounter = encounterRepo.findById(encounterId);
        if (optionalEncounter.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("encounter with id %d not found", encounterId));
        }
        return optionalEncounter.get();
    }

    @DeleteMapping(value = "/{encounterId}")
    @ResponseBody
    public int deleteEncounter(@PathVariable Integer encounterId) {
        return encounterRepo.deleteEncounter(encounterId);
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
            encounterRepo.save(encounter);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("cannot set description '%s' for encounter with id %d", description, encounterId));
        }
        return encounter;
    }
}
