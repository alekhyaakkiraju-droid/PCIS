package com.pcis.schema.migration;

/** Monetary column category from the data dictionary (WO-152). */
public enum MonetaryKind {
    /** Cent-level amounts (NUMERIC scale 2). */
    AMOUNT,
    /** Rate multipliers and factors (NUMERIC scale 4). */
    RATE_FACTOR
}
