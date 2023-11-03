package no.fintlabs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ConfigGroup {

    private String fintkontrollidattribute;
    private String prefix;
    private String suffix;
}
