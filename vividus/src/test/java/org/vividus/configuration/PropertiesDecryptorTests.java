/*
 * Copyright 2019-2021 the original author or authors.
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

package org.vividus.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertiesDecryptorTests
{
    private static final String PROPERTY_KEY = "key";
    private static final String ENCRYPTED_VALUE = "encrypted";
    private static final String DECRYPTED_VALUE = "decrypted";

    @Mock private StandardPBEStringEncryptor standardPBEStringEncryptor;

    @InjectMocks
    private PropertiesDecryptor propertiesDecryptor;

    @Test
    void shouldDecryptProperties()
    {
        Properties properties = new Properties();
        properties.setProperty(PROPERTY_KEY, "ENC(" + ENCRYPTED_VALUE + ")");
        when(standardPBEStringEncryptor.decrypt(ENCRYPTED_VALUE)).thenReturn(DECRYPTED_VALUE);
        Properties propertiesDecrypted = propertiesDecryptor.decryptProperties(properties);
        assertEquals(DECRYPTED_VALUE, propertiesDecrypted.getProperty(PROPERTY_KEY));
    }

    @Test
    void shouldKeepNonEncryptedPropertiesAsIs()
    {
        var propertyValue = "any";
        Properties properties = new Properties();
        properties.setProperty(PROPERTY_KEY, propertyValue);
        Properties propertiesDecrypted = propertiesDecryptor.decryptProperties(properties);
        assertEquals(propertyValue, propertiesDecrypted.getProperty(PROPERTY_KEY));
        verifyNoInteractions(standardPBEStringEncryptor);
    }

    @Test
    void shouldKeepNonStringPropertiesAsIs()
    {
        var propertyValue = new Object();
        Properties properties = new Properties();
        properties.put(PROPERTY_KEY, propertyValue);
        Properties propertiesDecrypted = propertiesDecryptor.decryptProperties(properties);
        assertEquals(propertyValue, propertiesDecrypted.get(PROPERTY_KEY));
        verifyNoInteractions(standardPBEStringEncryptor);
    }

    @Test
    void shouldDecryptPartsOfValue()
    {
        var decryptedFullValue = "username=open password=decrypted secret-key=decrypted";
        var encryptedFullValue = "username=open password=ENC(encrypted) secret-key=ENC(encrypted)";
        Properties properties = new Properties();
        properties.setProperty(PROPERTY_KEY, encryptedFullValue);
        when(standardPBEStringEncryptor.decrypt(ENCRYPTED_VALUE)).thenReturn(DECRYPTED_VALUE);
        Properties propertiesDecrypted = propertiesDecryptor.decryptProperties(properties);
        assertEquals(decryptedFullValue, propertiesDecrypted.getProperty(PROPERTY_KEY));
    }
}
