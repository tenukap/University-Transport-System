package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.Bus;
import com.bustrans.fleettrack.repository.BusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BusService {

    @Autowired
    private BusRepository busRepository;

    public Bus saveBus(Bus bus) {
        return busRepository.save(bus);
    }

    public List<Bus> getAllBuses() {
        return busRepository.findAll();
    }

    public Bus getBusById(Long id) {
        return busRepository.findById(id).orElse(null);
    }

    public void deleteBus(Long id) {
        busRepository.deleteById(id);
    }
}