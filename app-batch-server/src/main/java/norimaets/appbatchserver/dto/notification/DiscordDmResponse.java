package norimaets.appbatchserver.dto.notification;

public record DiscordDmResponse(
        DiscordDmStatus status
) {

    public boolean isCompleted() {
        return status == DiscordDmStatus.SENT
                || status == DiscordDmStatus.ALREADY_SENT
                || status == DiscordDmStatus.QUEUED;
    }
}