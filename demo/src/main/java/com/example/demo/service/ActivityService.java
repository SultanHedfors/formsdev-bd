package com.example.demo.service;

import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.dto.bsn_logic_dto.ProcedureDto;
import com.example.demo.entity.ActivityEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.mapper.ActivityMapper;
import com.example.demo.repository.ActivityRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;
    private final UserRepository userRepository;

    public ActivityService(ActivityRepository activityRepository, ActivityMapper activityMapper, UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.activityMapper = activityMapper;
        this.userRepository = userRepository;
    }

    public Page<ActivityDto> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recordAddedTime"));
        return activityRepository.findAll(pageable)
                .map(activityMapper::userRegistrationEntityToDto);
    }

    public ProcedureDto findById(Long id) {
        // TODO: Implement service logic to retrieve a procedure by id
        return null;
    }

    public ProcedureDto save(ProcedureDto procedureDto) {
        // TODO: Implement service logic to save a new procedure
        return procedureDto;
    }

    public ActivityDto markActivityAsOwn(ActivityDto activityDto, String username) {
        final ActivityEntity activityEntity = activityRepository.findById(activityDto.getActivityId())
                .orElseThrow(RuntimeException::new);

        final UserEntity user = userRepository.findByEmployeeCode(username).orElseThrow(RuntimeException::new);
        return activityDto;
    }

    public void delete(Long id) {
        // TODO: Implement service logic to delete a procedure by id
    }
}
