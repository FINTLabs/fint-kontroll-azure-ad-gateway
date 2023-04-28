package no.fintlabs;

import com.microsoft.graph.models.*;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
public class AzureUser extends BaseObject {
        private User userObject;

        public AzureUser(User user) {
                this.id = user.id;
                this.userObject = user;
        }
}




