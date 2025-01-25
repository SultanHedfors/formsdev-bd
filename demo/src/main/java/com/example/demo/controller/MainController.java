package com.example.demo.controller;

import com.example.demo.dto.EntryDto;
import com.example.demo.entity.EntryEntity;
import com.example.demo.mapper.EntryMapper;
import com.example.demo.service.EntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.swing.text.html.Option;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*") // Replace "*" with specific allowed origins
//@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MainController {

    EntryService entryService;
    EntryMapper entryMapper;

    public  MainController(EntryService entryService, EntryMapper entryMapper){
    this.entryService=entryService;
    this.entryMapper=entryMapper;
    }

    @GetMapping("/entries/{id}")
    public ResponseEntity<EntryDto> getEntries(@PathVariable long id){
        Optional<EntryEntity> entryOptional = entryService.handleGet(id);

        if(entryOptional.isPresent()){
            return ResponseEntity.ok(entryMapper.entryEntityToDto(entryOptional.get()));
        } else {
            return ResponseEntity.notFound().build();
        }

    }

    @PostMapping("/entries")
    public ResponseEntity<EntryDto> createEntries(@RequestBody EntryDto entryDto){
    entryService.handlePost(entryDto);
    return ResponseEntity.ok(entryDto);
    }
}
