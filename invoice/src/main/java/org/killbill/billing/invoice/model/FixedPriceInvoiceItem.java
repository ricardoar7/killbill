/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.invoice.model;

import java.math.BigDecimal;
import java.util.UUID;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.invoice.api.InvoiceItemType;
import org.killbill.billing.util.UUIDs;

public class FixedPriceInvoiceItem extends InvoiceItemCatalogBase {

    public FixedPriceInvoiceItem(final UUID invoiceId, final UUID accountId, @Nullable final UUID bundleId, @Nullable final UUID subscriptionId,
                                 final String productName, final String planName, final String phaseName,
                                 final LocalDate date, final BigDecimal amount, final Currency currency) {
        this(UUIDs.randomUUID(), null, invoiceId, accountId, bundleId, subscriptionId, productName, planName, phaseName, null, date, amount, currency);
    }

    public FixedPriceInvoiceItem(final UUID id, @Nullable final DateTime createdDate, final UUID invoiceId, final UUID accountId, final UUID bundleId,
                                 final UUID subscriptionId, final String productName, final String planName, final String phaseName,
                                 @Nullable final String description, final LocalDate date, final BigDecimal amount, final Currency currency) {
        super(id, createdDate, invoiceId, accountId, bundleId, subscriptionId, description, productName, planName, phaseName, null, date, null, amount, null, currency, null, InvoiceItemType.FIXED);
    }

    public FixedPriceInvoiceItem(final UUID id, @Nullable final DateTime createdDate, final UUID invoiceId, final UUID accountId, final UUID bundleId,
                                 final UUID subscriptionId, final String productName, final String planName, final String phaseName,
                                 final String prettyProductName, final String prettyPlanName, final String prettyPhaseName,
                                 @Nullable final String description, final LocalDate date, final BigDecimal amount, final Currency currency) {
        super(id, createdDate, invoiceId, accountId, bundleId, subscriptionId, description, productName, planName, phaseName, null, prettyProductName, prettyPlanName, prettyPhaseName,
              null, date, null, amount, null, currency, null, InvoiceItemType.FIXED);
    }

    @Override
    public String getDescription() {
        if (description != null) {
            return description;
        }

        final String resolvedPhaseName = getPrettyPhaseName() != null ? getPrettyPhaseName() : getPhaseName();
        if (resolvedPhaseName == null) {
            return "Fixed price charge";
        } else {
            if (getAmount().compareTo(BigDecimal.ZERO) == 0) {
                return getPhaseName();
            } else {
                return String.format("%s (fixed price)", resolvedPhaseName);
            }
        }
    }
}
