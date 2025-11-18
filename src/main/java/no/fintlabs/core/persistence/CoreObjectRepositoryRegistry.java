package no.fintlabs.core.persistence;

import no.fintlabs.core.entity.CoreObject;

import java.util.*;

public class CoreObjectRepositoryRegistry {

    private final Map<Class<?>, CoreObjectRepository> repositories = new HashMap<>();
    private final Map<Class<?>, List<Class<?>>> dependencies = new HashMap<>();

    public <T extends CoreObject> void register(Class<T> type, CoreObjectRepository<T> repository) {
        repositories.put(type, repository);
    }

    public <T extends CoreObject> void addDependency(Class<T> sourceType, Class<? extends CoreObject> dependentType) {
        dependencies.computeIfAbsent(sourceType, k -> new ArrayList<>()).add(dependentType);
    }

    @SuppressWarnings("unchecked")
    public <T extends CoreObject> CoreObjectRepository<T> getRepository(Class<T> type) {
        return (CoreObjectRepository<T>) repositories.get(type);
    }

    public List<CoreObjectRepository<?>> getDependentRepositories(Class<?> sourceType) {
        List<Class<?>> dependentTypes = dependencies.getOrDefault(sourceType, List.of());
        return dependentTypes.stream()
                .map(repositories::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
