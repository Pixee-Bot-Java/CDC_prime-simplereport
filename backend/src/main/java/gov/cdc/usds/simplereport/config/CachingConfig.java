package gov.cdc.usds.simplereport.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CachingConfig {

  public static final String DEVICE_MODEL_AND_TEST_PERFORMED_CODE_MAP =
      "deviceModelAndTestPerformedCodeMap";

  public static final String SPECIMEN_NAME_AND_SNOMED_MAP = "specimenTypeNameSNOMEDMap";

  @Bean
  public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager(
        DEVICE_MODEL_AND_TEST_PERFORMED_CODE_MAP, SPECIMEN_NAME_AND_SNOMED_MAP);
  }
}