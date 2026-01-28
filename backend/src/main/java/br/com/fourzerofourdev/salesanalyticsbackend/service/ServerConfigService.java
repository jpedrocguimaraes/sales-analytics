package br.com.fourzerofourdev.salesanalyticsbackend.service;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.ServerConfigDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.model.MonitoredServer;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.CurrencyType;
import br.com.fourzerofourdev.salesanalyticsbackend.repository.MonitoredServerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServerConfigService {

    private final MonitoredServerRepository monitoredServerRepository;

    public ServerConfigService(MonitoredServerRepository monitoredServerRepository) {
        this.monitoredServerRepository = monitoredServerRepository;
    }

    @Transactional
    public ServerConfigDTO createServer(ServerConfigDTO serverConfigDTO) {
        if(monitoredServerRepository.existsByName(serverConfigDTO.name())) {
            throw new IllegalArgumentException("A server with this name already exists.");
        }

        MonitoredServer server = MonitoredServer.builder()
                .name(serverConfigDTO.name())
                .type(serverConfigDTO.type())
                .currency(serverConfigDTO.currency() != null ? serverConfigDTO.currency() : CurrencyType.BRL)
                .salesUrl(serverConfigDTO.salesUrl())
                .serverAddress(serverConfigDTO.serverAddress())
                .active(serverConfigDTO.active())
                .build();

        return toDTO(monitoredServerRepository.save(server));
    }

    @Transactional(readOnly = true)
    public ServerConfigDTO getServerById(Long id) {
        return monitoredServerRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Server not found."));
    }

    @Transactional(readOnly = true)
    public List<ServerConfigDTO> getAllServers() {
        return monitoredServerRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public ServerConfigDTO updateServer(Long id, ServerConfigDTO serverConfigDTO) {
        MonitoredServer server = monitoredServerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Server not found."));

        server.setName(serverConfigDTO.name());
        server.setType(serverConfigDTO.type());
        server.setCurrency(serverConfigDTO.currency());
        server.setSalesUrl(serverConfigDTO.salesUrl());
        server.setServerAddress(serverConfigDTO.serverAddress());
        server.setActive(serverConfigDTO.active());

        return toDTO(monitoredServerRepository.save(server));
    }

    @Transactional
    public void deleteServer(Long id) {
        monitoredServerRepository.deleteById(id);
    }

    private ServerConfigDTO toDTO(MonitoredServer server) {
        return new ServerConfigDTO(
                server.getId(),
                server.getName(),
                server.getType(),
                server.getCurrency(),
                server.getSalesUrl(),
                server.getServerAddress(),
                server.isActive()
        );
    }
}