package com.pbuczek.pf.encounters.controller;

import com.pbuczek.pf.encounters.Encounter;
import com.pbuczek.pf.encounters.repository.EncounterRepository;
import com.pbuczek.pf.security.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/encounter")
public class EncounterController {

    EncounterRepository encounterRepo;

    @Autowired
    public EncounterController(EncounterRepository encounterRepo) {
        this.encounterRepo = encounterRepo;
    }

    @PostMapping
    @ResponseBody
    public Encounter createEncounter(@RequestBody Encounter newEncounter) {
        return encounterRepo.save(newEncounter);
    }

    @GetMapping()
    @ResponseBody
    public List<Encounter> readAllEncounters() {
        return encounterRepo.findAll();
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

}
