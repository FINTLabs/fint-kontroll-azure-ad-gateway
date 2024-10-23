package no.fintlabs.rest;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class LookupRestController {
    private final LookupService LookupService;

    @GetMapping("/{objectId}")
    public ResponseEntity<UserWithGroupsDto> getUserWithGroups(@PathVariable String objectId) {
        UserWithGroupsDto userWithGroups = LookupService.getAzureUserWithGroups(objectId);

        if (userWithGroups == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(userWithGroups);
    }
}


