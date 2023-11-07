package no.fintlabs;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfigGroup {

    private String fintkontrollidattribute;
    private String prefix;
    private String suffix;
}