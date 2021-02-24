package co.ratethem.service;

import co.ratethem.entity.Company;
import co.ratethem.entity.DouVacancy;
import co.ratethem.repository.CompanyRepository;
import co.ratethem.repository.DouVacancyRepository;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
public class ScrapCompanies {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired /* it is like config */
    private DouVacancyRepository douVacancyRepository;

    private List<String> douUrlSearchParams = Arrays.asList(
            "", /*no search param*/
            "&exp=0-1",
            "&exp=1-3" ,
            "&exp=3-5",
            "&exp=5plus");

    @PostConstruct
    public void init() {


        //add list of urls: Java, .Net, PHP, etc
        List<DouVacancy> vacancyList = douVacancyRepository.findAll();

        if(vacancyList.isEmpty()) {
            log.error("DOU vacancy list is Empty");
        } else {


            //Here we get such links as
            //https://jobs.dou.ua/vacancies/?category=Java
            //https://jobs.dou.ua/vacancies/?category=Python
            for (DouVacancy vacancy : vacancyList) {

                //Here we add search params to Url lik
                //https://jobs.dou.ua/vacancies/?category=Java&exp=0-1
                //https://jobs.dou.ua/vacancies/?category=Java&exp=1-3
                //https://jobs.dou.ua/vacancies/?category=Java&exp=3-5
                //https://jobs.dou.ua/vacancies/?category=Java&exp=5plus
                //etc
                for (String douUrlSearchParam : douUrlSearchParams) {

                    //try { Thread.sleep(1500); } catch (InterruptedException e) { log.error("Thread can't sleep 1500. Msg {}", e.getMessage());}

                    String url = vacancy.getUrl() + douUrlSearchParam;

                    log.debug("Current DOU url: {}", url);

                    //Here we get company Name and company Url
                    Map<String, String> companiesUrl = scrapCompanies(url);


                    for (Map.Entry<String, String> mapEntry : companiesUrl.entrySet()) {

                        //Here we check if company Name is already Exist in Database
                        boolean isExist = companyRepository.existsByName(mapEntry.getKey());

                        //if company Name does NOT Exist
                        if (isExist == false) {

                            //we fetch company Site Url, company Logo Url as Company entity
                            Optional<Company> companyOptional = getCompanyDataByDouUrl(mapEntry.getKey(), mapEntry.getValue());

                            if (companyOptional.isPresent()) {

                                //And Save New company into Database
                                companyRepository.save(companyOptional.get());
                            } else {
                                log.error("Couldn't fetch data for company: {} from url: {}", mapEntry.getKey(), mapEntry.getValue());
                            }

                        }
                        log.debug(mapEntry.getKey() + " - " + isExist);
                    }
                }
            }
        }

    }

    private Optional<Company> getCompanyDataByDouUrl(String companyName, String companyDOUurl) {

        try {
            Document doc = Jsoup.connect(companyDOUurl).get();
            String siteUrl = doc.select(".site").select("a").attr("href");
            String logoUrl = doc.select(".logo").attr("src");

            Company company = new Company();
            company.setName(companyName);
            company.setSiteUrl(siteUrl);
            company.setLogoUrl(logoUrl);
            company.setCreated(new Date() /*now ()*/);

            //todo: optimize return stmt
            return Optional.of(company);

        } catch (IOException e) {
            log.error("{} is not reachable. Error message: {}", companyDOUurl, e.getMessage());
        }

        return Optional.empty();
    }


    public Map<String, String> scrapCompanies(String url) {
        log.info("Url: {}", url);

        Map<String, String> companiesHrefs = new HashMap<>();
        try {
            Document doc = Jsoup.connect(url).get();

            Elements elements = doc.select(".company");

            companiesHrefs = elements.stream().map(e ->
                Pair.of(e.text(), e.attr("href"))
            ).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (companyName, companyUrl) -> {
                log.debug("duplicate key found!");
                return companyName;
            }));

        } catch (IOException e) {
            log.error(url + " is not reachable. Error message: " + e.getMessage());
        }

        return companiesHrefs;
    }
}
