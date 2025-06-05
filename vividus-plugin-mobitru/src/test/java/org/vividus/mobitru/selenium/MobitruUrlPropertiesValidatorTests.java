/*
 * Copyright 2019-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.mobitru.selenium;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.util.property.IPropertyParser;

@ExtendWith(MockitoExtension.class)
class MobitruUrlPropertiesValidatorTests
{
    private static final String PASS_VALUE = "mobitru_ak_vvd";
    private static final String PERSONAL = "personal";
    private static final String CORRECT_URL = "https://personal:mobitru_ak_vvd@app.mobitru.com/wd/hub";
    private static final String APP_MOBITRU_COM = "app.mobitru.com";
    private static final String SELENIUM_GRID_PREFIX = "selenium.grid.";
    private static final String HOST = "host";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String URL = "url";
    private static final String INCORRECT_PROPERTY_FOR_MOBITRU_MESSAGE =
            "Incorrect 'selenium.grid.%s' property for Mobitru: '%s'";

    @Mock private IPropertyParser propertyParser;
    @InjectMocks private MobitruUrlPropertiesValidator mobitruPropertiesValidator;

    @Test
    void shouldPassValidation()
    {
        when(propertyParser.getPropertyValuesByPrefix(SELENIUM_GRID_PREFIX)).thenReturn(
                Map.of(HOST, APP_MOBITRU_COM,
                       USERNAME, PERSONAL,
                       PASSWORD, PASS_VALUE,
                       URL, CORRECT_URL));
        assertDoesNotThrow(mobitruPropertiesValidator::validate);
    }

    @ParameterizedTest
    @MethodSource("exceptionProperties")
    void shouldThrowException(Map<String, String> properties, String failedPropertyName)
    {
        when(propertyParser.getPropertyValuesByPrefix(SELENIUM_GRID_PREFIX)).thenReturn(properties);
        var exception = assertThrows(IllegalStateException.class, mobitruPropertiesValidator::validate);
        String expectedExceptionMessage = String.format(INCORRECT_PROPERTY_FOR_MOBITRU_MESSAGE, failedPropertyName,
                properties.get(failedPropertyName));
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    static Stream<Arguments> exceptionProperties()
    {
        return Stream.of(
            Arguments.of(
                    Map.of(HOST, "localhost", USERNAME, PERSONAL, PASSWORD, PASS_VALUE,
                            URL, "https://personal:mobitru_ak_vvd@localhost/wd/hub"),
                    HOST),
            Arguments.of(
                    Map.of(HOST, APP_MOBITRU_COM, USERNAME, PERSONAL, PASSWORD, PASS_VALUE,
                            URL, "https://personal@app.mobitru.com/wd/hub"),
                    URL),
            Arguments.of(
                    Map.of(HOST, APP_MOBITRU_COM, USERNAME, PERSONAL, PASSWORD, PASS_VALUE,
                            URL, "https://app.mobitru.com/wd/hub"),
                    URL),
            Arguments.of(
                    Map.of(HOST, APP_MOBITRU_COM, USERNAME, "", PASSWORD, "",
                            URL, CORRECT_URL),
                    URL),
            Arguments.of(
                    Map.of(HOST, APP_MOBITRU_COM, USERNAME, PERSONAL, PASSWORD, PASS_VALUE,
                            URL, ""),
                    URL)
        );
    }
}
