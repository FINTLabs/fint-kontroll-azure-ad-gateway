package no.fintlabs.core.persistence;

import no.fintlabs.core.entity.CoreObject;

import java.util.*;

// group: CUD
// user: -
// userexternal: -
// device: -
// membership: CUD
// delta: CUD
// Persistor group: CUD - databasewriter
// MSGraph group: CUD - graph writer

public class CoreObjectRepositoryRegistry {

    /*private final Map<Class<?>, CoreObjectListRepository> repositories = new HashMap<>();
    private final Map<Class<?>, List<Class<?>>> dependencies = new HashMap<>();

    public <T extends CoreObject> void register(Class<T> type, CoreObjectListRepository<I, T> repository) {
        repositories.put(type, repository);
    }

    public <T extends CoreObject> void addDependency(Class<T> sourceType, Class<? extends CoreObject> dependentType) {
        dependencies.computeIfAbsent(sourceType, k -> new ArrayList<>()).add(dependentType);
    }

    @SuppressWarnings("unchecked")
    public <T extends CoreObject> CoreObjectListRepository<T> getRepository(Class<T> type) {
        return (CoreObjectListRepository<T>) repositories.get(type);
    }

    public List<CoreObjectListRepository<?>> getDependentRepositories(Class<?> sourceType) {
        List<Class<?>> dependentTypes = dependencies.getOrDefault(sourceType, List.of());
        return dependentTypes.stream()
                .map(repositories::get)
                .filter(Objects::nonNull)
                .toList();
    }*/
}
