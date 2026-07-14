package be.ephec.pdw.padel.scheduler;

import be.ephec.pdw.padel.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MatchScheduler {

    private final MatchService matchService;

    @Scheduled(fixedDelayString = "${app.scheduling.private-match-conversion-delay-ms:3600000}")
    public void convertirMatchsPrives() {
        matchService.convertirMatchsPrivesArrivesAUnJour();
    }
}
