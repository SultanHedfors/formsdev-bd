package com.example.demo.service;


import com.example.demo.dto.bsn_logic_dto.StatisticsDto;
import com.example.demo.dto.stats_dtos.*;
import com.example.demo.entity.UserEntity;
import com.example.demo.exception.CurrentUserNotFoundException;
import com.example.demo.mapper.EmployeeStatisticsMapper;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.stats.DailyEmployeeStatisticRepository;
import com.example.demo.repository.stats.MonthlyEmployeeStatisticRepository;
import com.example.demo.repository.stats.WeeklyEmployeeStatisticRepository;
import com.example.demo.repository.stats.YearlyEmployeeStatisticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final UserRepository userRepository;
    private final DailyEmployeeStatisticRepository   dailyRepo;
    private final WeeklyEmployeeStatisticRepository  weeklyRepo;
    private final MonthlyEmployeeStatisticRepository monthlyRepo;
    private final YearlyEmployeeStatisticRepository  yearlyRepo;
    private final EmployeeStatisticsMapper           mapper;

    public StatisticsDto getStatistics() {
        // 1) kto jest current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserDetails userDetails)) {
            throw new CurrentUserNotFoundException();
        }
        String username = userDetails.getUsername();

        // 2) pobierz wszystkich pracowników
        List<UserEntity> all = userRepository.findAll();

        // 3) zamapuj
        List<EmployeeStatsDto> stats = all.stream()
                .map(u -> {
                    Integer empId = u.getId();
                    String  code  = u.getFullName();
                    boolean isCurrent =u.getEmployeeCode().equals(username);

                    // 4) pobranie zestawów
                    List<DailyStatsDto>   daily   = mapper.toDailyDtoList(
                            dailyRepo.findTop100ByEmployeeIdOrderByStartDayDesc(empId)
                    );
                    List<WeeklyStatsDto>  weekly  = mapper.toWeeklyDtoList(
                            weeklyRepo.findTop36ByEmployeeIdOrderByWeekStartDesc(empId)
                    );
                    List<MonthlyStatsDto> monthly = mapper.toMonthlyDtoList(
                            monthlyRepo.findTop24ByEmployeeIdOrderByMonthDesc(empId)
                    );
                    List<YearlyStatsDto>  yearly  = mapper.toYearlyDtoList(
                            yearlyRepo.findTop5ByEmployeeIdOrderByYearDesc(empId)
                    );

                    return new EmployeeStatsDto(
                            code,
                            isCurrent,
                            daily,
                            weekly,
                            monthly,
                            yearly
                    );
                })
                // currentUser jako pierwszy, reszta w dowolnym porządku
                .sorted(Comparator.comparing(EmployeeStatsDto::isCurrentUser).reversed())
                .filter(e->!e.getDailyStats().isEmpty() ||
                        !e.getMonthlyStats().isEmpty() ||
                        !e.getWeeklyStats() .isEmpty() ||
                        !e.getYearlyStats().isEmpty())
                .collect(Collectors.toList());

        return new StatisticsDto(stats);
    }


}
