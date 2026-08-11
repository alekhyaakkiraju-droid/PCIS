package com.pcis.batch.reconciliation.api;

import com.pcis.batch.reconciliation.gate.CutoverGateScorecardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cutover")
public class CutoverScorecardController {

  private final CutoverGateScorecardService scorecardService;

  public CutoverScorecardController(CutoverGateScorecardService scorecardService) {
    this.scorecardService = scorecardService;
  }

  @GetMapping("/scorecard")
  public CutoverGateScorecardService.CutoverScorecard scorecard() {
    return scorecardService.scorecard();
  }
}
