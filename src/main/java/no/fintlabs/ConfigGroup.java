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
    public String fintkontrollidattribute;
    public String prefix;
    public String suffix;
    public boolean aslowercase;
}
