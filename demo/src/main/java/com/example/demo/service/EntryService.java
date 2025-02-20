package com.example.demo.service;

import com.example.demo.dto.EntryDto;
import com.example.demo.entity.EntryEntity;
import com.example.demo.mapper.EntryMapper;
import com.example.demo.repository.EntryRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EntryService {


    EntryRepository entryRepository;
    EntryMapper entryMapper;

    public EntryService(EntryRepository entryRepository, EntryMapper entryMapper) {
        this.entryRepository = entryRepository;
        this.entryMapper = entryMapper;
    }

    public void handlePost(EntryDto entryDto) {
        entryRepository.save(entryMapper.entryDtoToEntity(entryDto));
    }

    public Optional<EntryEntity> handleGet(long id) {
        return entryRepository.findById(id);
    }
}
