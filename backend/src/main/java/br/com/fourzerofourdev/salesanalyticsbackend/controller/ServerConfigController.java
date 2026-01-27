package br.com.fourzerofourdev.salesanalyticsbackend.controller;

import br.com.fourzerofourdev.salesanalyticsbackend.dto.ServerConfigDTO;
import br.com.fourzerofourdev.salesanalyticsbackend.service.ServerConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers")
@CrossOrigin(origins = "http://localhost:4200")
public class ServerConfigController {

    private final ServerConfigService serverConfigService;

    public ServerConfigController(ServerConfigService serverConfigService) {
        this.serverConfigService = serverConfigService;
    }

    @PostMapping
    public ResponseEntity<ServerConfigDTO> createServer(@RequestBody ServerConfigDTO serverConfigDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serverConfigService.createServer(serverConfigDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerConfigDTO> getServerById(@PathVariable Long id) {
        return ResponseEntity.ok(serverConfigService.getServerById(id));
    }

    @GetMapping
    public List<ServerConfigDTO> getAllServers() {
        return serverConfigService.getAllServers();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServerConfigDTO> updateServer(@PathVariable Long id, @RequestBody ServerConfigDTO serverConfigDTO) {
        return ResponseEntity.ok(serverConfigService.updateServer(id, serverConfigDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        serverConfigService.deleteServer(id);
        return ResponseEntity.noContent().build();
    }
}