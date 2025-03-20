package com.example.demo.service;


import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class OwnProcedureService {

    public List<ActivityDto> findAll() {
        // TODO: Implement service logic to retrieve all own procedures
        return Collections.emptyList();
    }

    public ActivityDto save(ActivityDto activityDto) {
        // TODO: Implement service logic to save a new own procedure
        return activityDto;
    }

    public void delete(Long id) {
        // TODO: Implement service logic to delete an own procedure by id
    }
}
