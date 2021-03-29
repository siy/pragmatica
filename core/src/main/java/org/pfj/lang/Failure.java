/*
 * Copyright (c) 2021 Sergiy Yevtushenko.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pfj.lang;

import java.text.MessageFormat;
import java.util.Objects;

/**
 * Basic interface for failure types.
 */
public interface Failure {
	/**
	 * Message associated with the failure.
	 */
	String message();

	/**
	 * Construct a failure with the message build using {@link MessageFormat}.
	 *
	 * @param format message format
	 * @param params message parameters
	 *
	 * @return created instance
	 *
	 * @see MessageFormat
	 */
	static Failure failure(String format, Object... params) {
		return failure(MessageFormat.format(format, params));
	}

	/**
	 * Construct a simple failure with a given message.
	 *
	 * @param message failure message
	 *
	 * @return created instance
	 */
	static Failure failure(String message) {
		return new Failure() {
			@Override
			public String message() {
				return message;
			}

			@Override
			public int hashCode() {
				return message.hashCode();
			}

			@Override
			public boolean equals(Object obj) {
				if (obj == this) {
					return true;
				}

				return (obj instanceof Failure failure) && Objects.equals(failure.message(), message);
			}

			@Override
			public String toString() {
				return "Failure(\"" + message + "\")";
			}
		};
	}
}
