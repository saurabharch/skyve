package org.skyve.domain.types.converters.timestamp;

import org.skyve.domain.messages.ConversionException;

public class MM_DD_YYYY_HH24_MI_SS extends AbstractTimestampConverter {
	public static final String PATTERN = "MM/dd/yyyy HH:mm:ss";

	@Override
	protected String getPattern() {
		return PATTERN;
	}
	
	@Override
	protected String getI18nKey() {
		return ConversionException.MM_DD_YYYY_HH24_MI_SS_KEY;
	}
}
