package gov.cdc.usds.simplereport.service;

import static gov.cdc.usds.simplereport.config.CachingConfig.DEVICE_MODEL_AND_TEST_PERFORMED_CODE_MAP;
import static gov.cdc.usds.simplereport.config.CachingConfig.SPECIMEN_NAME_AND_SNOMED_MAP;

import gov.cdc.usds.simplereport.db.model.DeviceType;
import gov.cdc.usds.simplereport.db.model.SpecimenType;
import gov.cdc.usds.simplereport.db.repository.DeviceTypeRepository;
import gov.cdc.usds.simplereport.db.repository.SpecimenTypeRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultsUploaderDeviceValidationService {
  private final DeviceTypeRepository deviceTypeRepository;
  private final SpecimenTypeRepository specimenTypeRepository;

  private static final Map<String, String> specimenSNOMEDMap =
      Map.ofEntries(
          Map.entry("swab of internal nose", "445297001"),
          Map.entry("nasal swab", "445297001"),
          Map.entry("nasal", "445297001"),
          Map.entry("varied", "445297001"),
          Map.entry("nasopharyngeal swab", "258500001"),
          Map.entry("mid-turbinate nasal swab", "871810001"),
          Map.entry("anterior nares swab", "697989009"),
          Map.entry("anterior nasal swab", "697989009"),
          Map.entry("nasopharyngeal aspirate", "258411007"),
          Map.entry("nasopharyngeal washings", "258467004"),
          Map.entry("nasopharyngeal wash", "258467004"),
          Map.entry("nasal aspirate", "429931000124105"),
          Map.entry("nasal aspirate specimen", "429931000124105"),
          Map.entry("throat swab", "258529004"),
          Map.entry("oropharyngeal swab", "258529004"),
          Map.entry("oral swab", "418932006"),
          Map.entry("sputum specimen", "119334006"),
          Map.entry("sputum", "119334006"),
          Map.entry("saliva specimen", "258560004"),
          Map.entry("saliva", "258560004"),
          Map.entry("serum specimen", "119364003"),
          Map.entry("serum", "119364003"),
          Map.entry("plasma specimen", "119361006"),
          Map.entry("plasma", "119361006"),
          Map.entry("whole blood sample", "258580003"),
          Map.entry("whole blood", "258580003"),
          Map.entry("venous blood specimen", "122555007"),
          Map.entry("venous whole blood", "122555007"),
          Map.entry("blood specimen", "119297000"),
          Map.entry("capillary blood specimen", "122554006"),
          Map.entry("fingerstick whole blood", "122554006"),
          Map.entry("dried blood spot specimen", "440500007"),
          Map.entry("dried blood spot", "440500007"),
          Map.entry("fingerstick blood dried blood spot", "440500007"),
          Map.entry("nasopharyngeal and oropharyngeal swab", "433801000124107"),
          Map.entry("nasal and throat swab combination", "433801000124107"),
          Map.entry("nasal and throat swab", "433801000124107"),
          Map.entry("lower respiratory fluid sample", "309171007"),
          Map.entry("lower respiratory tract aspirates", "309171007"),
          Map.entry("bronchoalveolar lavage fluid sample", "258607008"),
          Map.entry("bronchoalveolar lavage fluid", "258607008"),
          Map.entry("bronchoalveolar lavage", "258607008"));

  @Cacheable(DEVICE_MODEL_AND_TEST_PERFORMED_CODE_MAP)
  public Map<String, DeviceType> getModelAndTestPerformedCodeToDeviceMap() {
    log.info("generating ModelAndTestPerformedCodeToDeviceMap cache");

    Map<String, DeviceType> resultMap = new HashMap<>();

    deviceTypeRepository
        .findAllRecords()
        .forEach(
            deviceType ->
                deviceType
                    .getSupportedDiseaseTestPerformed()
                    .forEach(
                        deviceTypeDisease -> {
                          String model = deviceType.getModel();
                          String testPerformedCode = deviceTypeDisease.getTestPerformedLoincCode();
                          if (model != null && testPerformedCode != null) {
                            resultMap.put(getMapKey(model, testPerformedCode), deviceType);
                          }
                        }));

    return resultMap;
  }

  @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
  @Caching(
      evict = {@CacheEvict(value = DEVICE_MODEL_AND_TEST_PERFORMED_CODE_MAP, allEntries = true)})
  public void cacheModelAndTestPerformedCodeToDeviceMap() {
    log.info("clear and generate ModelAndTestPerformedCodeToDeviceMap cache");
    getModelAndTestPerformedCodeToDeviceMap();
  }

  @Cacheable(SPECIMEN_NAME_AND_SNOMED_MAP)
  public Map<String, String> getSpecimenTypeNameToSNOMEDMap() {
    log.info("generating getSpecimenTypeNameToSNOMEDMap cache");

    var dbSpecimens =
        specimenTypeRepository.findAll().stream()
            .collect(Collectors.toMap(SpecimenType::getName, SpecimenType::getTypeCode));

    // Combine specimen name -> SNOMED maps from the database and those hardcoded in the service,
    // favoring the DB result on conflicts
    return Stream.of(dbSpecimens, specimenSNOMEDMap)
        .flatMap(map -> map.entrySet().stream())
        .collect(
            Collectors.toMap(
                o -> o.getKey().toLowerCase(), Map.Entry::getValue, (db, mem) -> db, HashMap::new));
  }

  @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
  @Caching(evict = {@CacheEvict(value = SPECIMEN_NAME_AND_SNOMED_MAP, allEntries = true)})
  public void cacheSpecimenTypeNameToSNOMEDMap() {
    log.info("clear and generate specimenTypeNameToSNOMEDMap cache");
    getSpecimenTypeNameToSNOMEDMap();
  }

  public static String getMapKey(String model, String testPerformedCode) {
    return model.toLowerCase() + "|" + testPerformedCode.toLowerCase();
  }
}