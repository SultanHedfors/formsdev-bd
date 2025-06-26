package com.example.demo.grpc;

import com.example.demo.mapper.ProtoObjectsMapping;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Slf4j
public class GrpcSendSchedulesClient {

    private final SendSchedulesServiceGrpc.SendSchedulesServiceBlockingStub stub;


    public GrpcSendSchedulesClient(@Value("${microservice.report.creator.url.host}") String extServiceHost,
                                   @Value("${microservice.report.creator.url.port}") int extServicePort) {

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(extServiceHost, extServicePort)
                .build();
        this.stub = SendSchedulesServiceGrpc.newBlockingStub(channel);
    }

    public void sendSchedulesRequest(List<Schedules.WorkSchedule> workSchedules) {
        var request = Schedules.SchedulesRequest.newBuilder()
                .addAllScheduleRows(workSchedules)
                .build();
        var response = stub.sendSchedules(request);
        log.info(response.getMessage());
    }

}
