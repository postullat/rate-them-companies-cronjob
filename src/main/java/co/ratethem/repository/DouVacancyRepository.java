package co.ratethem.repository;

import co.ratethem.entity.DouVacancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DouVacancyRepository extends JpaRepository<DouVacancy, Long> {


}
