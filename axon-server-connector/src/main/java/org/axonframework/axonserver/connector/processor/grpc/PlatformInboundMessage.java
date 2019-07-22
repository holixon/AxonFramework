/*
 * Copyright (c) 2010-2019. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.axonserver.connector.processor.grpc;

import io.axoniq.axonserver.grpc.control.PlatformInboundInstruction;

/**
 * Supplier of {@link PlatformInboundInstruction}.
 *
 * @author Sara Pellegrini
 * @since 4.0
 */
@FunctionalInterface
public interface PlatformInboundMessage {

    /**
     * Supply a {@link PlatformInboundInstruction}.
     *
     * @return a {@link PlatformInboundInstruction}
     */
    PlatformInboundInstruction instruction();
}
