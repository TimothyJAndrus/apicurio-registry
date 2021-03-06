/*
 * Copyright 2020 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.registry.utils.serde;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import io.apicurio.registry.client.RegistryService;
import io.apicurio.registry.common.proto.Serde;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;

/**
 * @author Ales Justin
 * @author Hiram Chirino
 */
public class ProtobufKafkaDeserializer extends AbstractKafkaDeserializer<byte[], DynamicMessage, ProtobufKafkaDeserializer> {
    public ProtobufKafkaDeserializer() {
    }

    public ProtobufKafkaDeserializer(RegistryService client) {
        super(client);
    }

    @Override
    protected byte[] toSchema(Response response) {
        return response.readEntity(byte[].class);
    }

    @Override
    protected DynamicMessage readData(byte[] schema, ByteBuffer buffer, int start, int length) {
        try {
            Serde.Schema s = Serde.Schema.parseFrom(schema);
            Descriptors.FileDescriptor fileDescriptor = toFileDescriptor(s);

            byte[] bytes = new byte[length];
            System.arraycopy(buffer.array(), start, bytes, 0, length);
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);

            Serde.Ref ref = Serde.Ref.parseDelimitedFrom(is);

            Descriptors.Descriptor descriptor = fileDescriptor.findMessageTypeByName(ref.getName());
            return DynamicMessage.parseFrom(descriptor, is);
        } catch (IOException | Descriptors.DescriptorValidationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Descriptors.FileDescriptor toFileDescriptor(Serde.Schema s) throws Descriptors.DescriptorValidationException {
        List<Descriptors.FileDescriptor> imports = new ArrayList<>();
        for (Serde.Schema i : s.getImportList()) {
            imports.add(toFileDescriptor(i));
        }
        return Descriptors.FileDescriptor.buildFrom(s.getFile(), imports.toArray(new Descriptors.FileDescriptor[0]));
    }
}
