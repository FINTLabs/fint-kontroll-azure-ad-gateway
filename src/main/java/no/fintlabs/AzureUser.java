package no.fintlabs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class AzureUser {
        private String userPrincipalName;
        private String id;
        private String mail;
}