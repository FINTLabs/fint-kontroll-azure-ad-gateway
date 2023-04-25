package no.fintlabs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

//@Service
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ConfigUser {

    private List<String> optionaluserattributes = Collections.emptyList();
    private String employeeidattribute;
    private String studentidattribute;


}