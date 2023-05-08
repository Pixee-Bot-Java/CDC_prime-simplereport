package gov.cdc.usds.simplereport.service;

import static gov.cdc.usds.simplereport.config.CachingConfig.DEVICE_MODEL_AND_TEST_PERFORMED_CODE_SET;

import gov.cdc.usds.simplereport.db.repository.DeviceTypeRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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

  @Cacheable(DEVICE_MODEL_AND_TEST_PERFORMED_CODE_SET)
  public Set<String> getModelAndTestPerformedCodeSet() {
    log.info("generating ModelAndTestPerformedCodeSet cache");

    Set<String> resultSet = new HashSet<>();

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
                            resultSet.add(getSetKey(model, testPerformedCode));
                          }
                        }));

    return resultSet;
  }

  @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
  @Caching(
      evict = {@CacheEvict(value = DEVICE_MODEL_AND_TEST_PERFORMED_CODE_SET, allEntries = true)})
  public void cacheModelAndTestPerformedCodeSet() {
    log.info("clear and generate ModelAndTestPerformedCodeSet cache");
    getModelAndTestPerformedCodeSet();
  }

  public static String getSetKey(String model, String testPerformedCode) {
    return model.toLowerCase() + "|" + testPerformedCode.toLowerCase();
  }
}
