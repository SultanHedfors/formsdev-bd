package com.example.demo.service;


import com.example.demo.dto.bsn_logic_dto.StatisticsDto;
import com.example.demo.dto.stats_dtos.*;
import com.example.demo.entity.UserEntity;
import com.example.demo.mapper.EmployeeStatisticsMapper;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.stats.DailyEmployeeStatisticRepository;
import com.example.demo.repository.stats.MonthlyEmployeeStatisticRepository;
import com.example.demo.repository.stats.WeeklyEmployeeStatisticRepository;
import com.example.demo.repository.stats.YearlyEmployeeStatisticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.example.demo.util.AuthUtil.userFromSecurityContext;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final UserRepository userRepository;
    private final DailyEmployeeStatisticRepository dailyRepo;
    private final WeeklyEmployeeStatisticRepository weeklyRepo;
    private final MonthlyEmployeeStatisticRepository monthlyRepo;
    private final YearlyEmployeeStatisticRepository yearlyRepo;
    private final EmployeeStatisticsMapper mapper;

    public StatisticsDto getStatistics() {

        String currentUser = userFromSecurityContext();

        Set<UserEntity> allUsers = new HashSet<>(userRepository.findAll());

        List<EmployeeStatsDto> stats = allUsers.stream()
                .map(u -> {
                    Integer empId = u.getId();
                    String name = u.getFullName();
                    String code = u.getEmployeeCode();
                    boolean isCurrentlyLogged = u.getEmployeeCode().equals(currentUser);

                    List<DailyStatsDto> daily = mapper.toDailyDtoList(
                            dailyRepo.findTop100ByEmployeeIdOrderByStartDayDesc(empId)
                    );
                    List<WeeklyStatsDto> weekly = mapper.toWeeklyDtoList(
                            weeklyRepo.findTop36ByEmployeeIdOrderByWeekStartDesc(empId)
                    );
                    List<MonthlyStatsDto> monthly = mapper.toMonthlyDtoList(
                            monthlyRepo.findTop24ByEmployeeIdOrderByMonthDesc(empId)
                    );
                    List<YearlyStatsDto> yearly = mapper.toYearlyDtoList(
                            yearlyRepo.findTop5ByEmployeeIdOrderByYearDesc(empId)
                    );

                    return new EmployeeStatsDto(
                            name,
                            code,
                            isCurrentlyLogged,
                            daily,
                            weekly,
                            monthly,
                            yearly
                    );
                })

                .sorted(Comparator.comparing(EmployeeStatsDto::isCurrentUser).reversed())
                .filter(e -> !e.getDailyStats().isEmpty() ||
                        !e.getMonthlyStats().isEmpty() ||
                        !e.getWeeklyStats().isEmpty() ||
                        !e.getYearlyStats().isEmpty())
                .toList();

        return new StatisticsDto(stats);
    }


}
