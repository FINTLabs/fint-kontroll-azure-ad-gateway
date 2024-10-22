package no.fintlabs.rest;

import com.microsoft.graph.models.User;

import java.util.List;

public record UserWithGroupsDto(User user, List<String> groups) {
}

