package com.pcis.configsvc.api;

import com.pcis.configsvc.api.dto.TunableHistoryResponse;
import com.pcis.configsvc.api.dto.TunableResponse;
import com.pcis.configsvc.api.dto.UpdateTunableRequest;
import com.pcis.configsvc.application.AdminTunableService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/tunables")
public class AdminTunableController {

  private final AdminTunableService adminTunableService;

  public AdminTunableController(AdminTunableService adminTunableService) {
    this.adminTunableService = adminTunableService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('configuration-admin')")
  public Page<TunableResponse> list(@PageableDefault(size = 50) Pageable pageable) {
    return adminTunableService.listCurrent(pageable);
  }

  @GetMapping("/{key}/history")
  @PreAuthorize("hasAuthority('configuration-admin')")
  public List<TunableHistoryResponse> history(@PathVariable("key") String key) {
    return adminTunableService.history(key);
  }

  @PutMapping("/{key}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAuthority('configuration-admin')")
  public TunableResponse update(
      @PathVariable("key") String key, @Valid @RequestBody UpdateTunableRequest request) {
    return adminTunableService.update(key, request);
  }
}
