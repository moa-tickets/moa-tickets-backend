package settings.support.fixture;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public abstract class BaseFixture<T, ID> {
    protected abstract JpaRepository<T, ID> repo();

    public T save(T entity) {
        return repo().save(entity);
    }

    public T findById(ID id) {
        return repo().findById(id).orElseThrow();
    }

    public void clear() {
        repo().deleteAllInBatch();
    }

}
