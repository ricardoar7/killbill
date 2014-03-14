/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
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

package org.killbill.billing.invoice;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.mockito.Mockito;
import org.skife.jdbi.v2.IDBI;
import org.testng.Assert;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.account.api.AccountInternalApi;
import org.killbill.billing.account.api.AccountUserApi;
import org.killbill.billing.callcontext.InternalCallContext;
import org.killbill.billing.callcontext.InternalTenantContext;
import org.killbill.billing.catalog.MockPlan;
import org.killbill.billing.catalog.MockPlanPhase;
import org.killbill.billing.catalog.api.BillingPeriod;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.catalog.api.Plan;
import org.killbill.billing.catalog.api.PlanPhase;
import org.killbill.clock.Clock;
import org.killbill.commons.locker.GlobalLocker;
import org.killbill.billing.entity.EntityPersistenceException;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceApiException;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoiceNotifier;
import org.killbill.billing.invoice.api.InvoicePayment;
import org.killbill.billing.invoice.dao.InvoiceDao;
import org.killbill.billing.invoice.dao.InvoiceItemModelDao;
import org.killbill.billing.invoice.dao.InvoiceItemSqlDao;
import org.killbill.billing.invoice.dao.InvoiceModelDao;
import org.killbill.billing.invoice.dao.InvoiceModelDaoHelper;
import org.killbill.billing.invoice.dao.InvoicePaymentModelDao;
import org.killbill.billing.invoice.dao.InvoicePaymentSqlDao;
import org.killbill.billing.invoice.generator.InvoiceGenerator;
import org.killbill.billing.invoice.notification.NullInvoiceNotifier;
import org.killbill.billing.junction.BillingEvent;
import org.killbill.billing.junction.BillingEventSet;
import org.killbill.billing.junction.BillingInternalApi;
import org.killbill.billing.junction.BillingModeType;
import org.killbill.billing.mock.MockAccountBuilder;
import org.killbill.billing.subscription.api.SubscriptionBase;
import org.killbill.billing.subscription.api.SubscriptionBaseInternalApi;
import org.killbill.billing.subscription.api.SubscriptionBaseTransitionType;
import org.killbill.billing.subscription.api.user.SubscriptionBaseApiException;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.InternalCallContextFactory;
import org.killbill.billing.util.currency.KillBillMoney;
import org.killbill.billing.util.dao.NonEntityDao;
import org.killbill.billing.util.svcsapi.bus.BusService;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class TestInvoiceHelper {

    public static final Currency accountCurrency = Currency.USD;

    public static final BigDecimal ZERO = new BigDecimal("0.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal ONE_HALF = new BigDecimal("0.5").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal ONE = new BigDecimal("1.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal ONE_AND_A_HALF = new BigDecimal("1.5").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal TWO = new BigDecimal("2.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal THREE = new BigDecimal("3.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal FOUR = new BigDecimal("4.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal FIVE = new BigDecimal("5.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal SIX = new BigDecimal("6.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal SEVEN = new BigDecimal("7.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal EIGHT = new BigDecimal("8.0").setScale(KillBillMoney.MAX_SCALE);

    public static final BigDecimal TEN = new BigDecimal("10.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal ELEVEN = new BigDecimal("11.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal TWELVE = new BigDecimal("12.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal THIRTEEN = new BigDecimal("13.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal FOURTEEN = new BigDecimal("14.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal FIFTEEN = new BigDecimal("15.0").setScale(KillBillMoney.MAX_SCALE);

    public static final BigDecimal NINETEEN = new BigDecimal("19.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal TWENTY = new BigDecimal("20.0").setScale(KillBillMoney.MAX_SCALE);

    public static final BigDecimal TWENTY_FOUR = new BigDecimal("24.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal TWENTY_FIVE = new BigDecimal("25.0").setScale(KillBillMoney.MAX_SCALE);

    public static final BigDecimal TWENTY_EIGHT = new BigDecimal("28.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal TWENTY_NINE = new BigDecimal("29.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal THIRTY = new BigDecimal("30.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal THIRTY_ONE = new BigDecimal("31.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal THIRTY_THREE = new BigDecimal("33.0").setScale(KillBillMoney.MAX_SCALE);

    public static final BigDecimal FORTY = new BigDecimal("40.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal SIXTY_SIX = new BigDecimal("66.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal SEVENTY_FIVE = new BigDecimal("75.0").setScale(KillBillMoney.MAX_SCALE);

    public static final BigDecimal EIGHTY_NINE = new BigDecimal("89.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal NINETY = new BigDecimal("90.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal NINETY_ONE = new BigDecimal("91.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal NINETY_TWO = new BigDecimal("92.0").setScale(KillBillMoney.MAX_SCALE);

    public static final BigDecimal ONE_HUNDRED = new BigDecimal("100.0").setScale(KillBillMoney.MAX_SCALE);

    public static final BigDecimal THREE_HUNDRED_AND_FOURTY_NINE = new BigDecimal("349.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal THREE_HUNDRED_AND_FIFTY_FOUR = new BigDecimal("354.0").setScale(KillBillMoney.MAX_SCALE);

    public static final BigDecimal THREE_HUNDRED_AND_SIXTY_FIVE = new BigDecimal("365.0").setScale(KillBillMoney.MAX_SCALE);
    public static final BigDecimal THREE_HUNDRED_AND_SIXTY_SIX = new BigDecimal("366.0").setScale(KillBillMoney.MAX_SCALE);

    private final InvoiceGenerator generator;
    private final BillingInternalApi billingApi;
    private final AccountInternalApi accountApi;
    private final AccountUserApi accountUserApi;
    private final SubscriptionBaseInternalApi subscriptionApi;
    private final BusService busService;
    private final InvoiceDao invoiceDao;
    private final GlobalLocker locker;
    private final Clock clock;
    private final InternalCallContext internalCallContext;
    private final NonEntityDao nonEntityDao;
    private final InternalCallContextFactory internalCallContextFactory;

    // Low level SqlDao used by the tests to directly insert rows
    private final InvoicePaymentSqlDao invoicePaymentSqlDao;
    private final InvoiceItemSqlDao invoiceItemSqlDao;

    @Inject
    public TestInvoiceHelper(final InvoiceGenerator generator, final IDBI dbi,
                             final BillingInternalApi billingApi, final AccountInternalApi accountApi, final AccountUserApi accountUserApi, final SubscriptionBaseInternalApi subscriptionApi, final BusService busService,
                             final InvoiceDao invoiceDao, final GlobalLocker locker, final Clock clock, final NonEntityDao nonEntityDao, final InternalCallContext internalCallContext,
                             final InternalCallContextFactory internalCallContextFactory) {
        this.generator = generator;
        this.billingApi = billingApi;
        this.accountApi = accountApi;
        this.accountUserApi = accountUserApi;
        this.subscriptionApi = subscriptionApi;
        this.busService = busService;
        this.invoiceDao = invoiceDao;
        this.locker = locker;
        this.clock = clock;
        this.nonEntityDao = nonEntityDao;
        this.internalCallContext = internalCallContext;
        this.internalCallContextFactory = internalCallContextFactory;
        this.invoiceItemSqlDao = dbi.onDemand(InvoiceItemSqlDao.class);
        this.invoicePaymentSqlDao = dbi.onDemand(InvoicePaymentSqlDao.class);
    }

    public UUID generateRegularInvoice(final Account account, final DateTime targetDate, final CallContext callContext) throws Exception {
        final SubscriptionBase subscription = Mockito.mock(SubscriptionBase.class);
        Mockito.when(subscription.getId()).thenReturn(UUID.randomUUID());
        Mockito.when(subscription.getBundleId()).thenReturn(new UUID(0L, 0L));
        final BillingEventSet events = new MockBillingEventSet();
        final Plan plan = MockPlan.createBicycleNoTrialEvergreen1USD();
        final PlanPhase planPhase = MockPlanPhase.create1USDMonthlyEvergreen();
        final DateTime effectiveDate = new DateTime().minusDays(1);
        final Currency currency = Currency.USD;
        final BigDecimal fixedPrice = null;
        events.add(createMockBillingEvent(account, subscription, effectiveDate, plan, planPhase,
                                          fixedPrice, BigDecimal.ONE, currency, BillingPeriod.MONTHLY, 1,
                                          BillingModeType.IN_ADVANCE, "", 1L, SubscriptionBaseTransitionType.CREATE));

        Mockito.when(billingApi.getBillingEventsForAccountAndUpdateAccountBCD(Mockito.<UUID>any(), Mockito.<InternalCallContext>any())).thenReturn(events);

        final InvoiceNotifier invoiceNotifier = new NullInvoiceNotifier();
        final InvoiceDispatcher dispatcher = new InvoiceDispatcher(generator, accountApi, billingApi, subscriptionApi,
                                                                   invoiceDao, nonEntityDao, invoiceNotifier, locker, busService.getBus(),
                                                                   clock);

        Invoice invoice = dispatcher.processAccount(account.getId(), targetDate, true, internalCallContext);
        Assert.assertNotNull(invoice);

        final InternalCallContext context = internalCallContextFactory.createInternalCallContext(account.getId(), callContext);

        List<InvoiceModelDao> invoices = invoiceDao.getInvoicesByAccount(context);
        Assert.assertEquals(invoices.size(), 0);

        invoice = dispatcher.processAccount(account.getId(), targetDate, false, context);
        Assert.assertNotNull(invoice);

        invoices = invoiceDao.getInvoicesByAccount(context);
        Assert.assertEquals(invoices.size(), 1);

        return invoice.getId();
    }

    public SubscriptionBase createSubscription() throws SubscriptionBaseApiException {
        UUID uuid = UUID.randomUUID();
        final SubscriptionBase subscription = Mockito.mock(SubscriptionBase.class);
        Mockito.when(subscription.getId()).thenReturn(uuid);
        Mockito.when(subscriptionApi.getSubscriptionFromId(Mockito.<UUID>any(), Mockito.<InternalTenantContext>any())).thenReturn(subscription);
        return subscription;
    }

    public Account createAccount(final CallContext callContext) throws AccountApiException {
        final AccountData accountData = new MockAccountBuilder().name(UUID.randomUUID().toString().substring(1, 8))
                                                                .firstNameLength(6)
                                                                .email(UUID.randomUUID().toString().substring(1, 8))
                                                                .phone(UUID.randomUUID().toString().substring(1, 8))
                                                                .migrated(false)
                                                                .isNotifiedForInvoices(true)
                                                                .externalKey(UUID.randomUUID().toString().substring(1, 8))
                                                                .billingCycleDayLocal(31)
                                                                .currency(accountCurrency)
                                                                .paymentMethodId(UUID.randomUUID())
                                                                .timeZone(DateTimeZone.UTC)
                                                                .build();
        return accountUserApi.createAccount(accountData, callContext);
    }

    public void createInvoiceItem(final InvoiceItem invoiceItem, final InternalCallContext internalCallContext) throws EntityPersistenceException {
        invoiceItemSqlDao.create(new InvoiceItemModelDao(invoiceItem), internalCallContext);
    }

    public InvoiceItemModelDao getInvoiceItemById(final UUID invoiceItemId, final InternalCallContext internalCallContext) {
        return invoiceItemSqlDao.getById(invoiceItemId.toString(), internalCallContext);
    }

    public List<InvoiceItemModelDao> getInvoiceItemBySubscriptionId(final UUID subscriptionId, final InternalCallContext internalCallContext) {
        return invoiceItemSqlDao.getInvoiceItemsBySubscription(subscriptionId.toString(), internalCallContext);
    }

    public List<InvoiceItemModelDao> getInvoiceItemByAccountId(final InternalCallContext internalCallContext) {
        return invoiceItemSqlDao.getByAccountRecordId(internalCallContext);
    }

    public List<InvoiceItemModelDao> getInvoiceItemByInvoiceId(final UUID invoiceId, final InternalCallContext internalCallContext) {
        return invoiceItemSqlDao.getInvoiceItemsByInvoice(invoiceId.toString(), internalCallContext);
    }

    public void createInvoice(final Invoice invoice, final boolean isRealInvoiceWithItems, final InternalCallContext internalCallContext) {
        final InvoiceModelDao invoiceModelDao = new InvoiceModelDao(invoice);
        final List<InvoiceItemModelDao> invoiceItemModelDaos = ImmutableList.<InvoiceItemModelDao>copyOf(Collections2.transform(invoice.getInvoiceItems(),
                                                                                                                                new Function<InvoiceItem, InvoiceItemModelDao>() {
                                                                                                                                    @Override
                                                                                                                                    public InvoiceItemModelDao apply(final InvoiceItem input) {
                                                                                                                                        return new InvoiceItemModelDao(input);
                                                                                                                                    }
                                                                                                                                }));
        // Not really needed, there shouldn't be any payment at this stage
        final List<InvoicePaymentModelDao> invoicePaymentModelDaos = ImmutableList.<InvoicePaymentModelDao>copyOf(Collections2.transform(invoice.getPayments(),
                                                                                                                                         new Function<InvoicePayment, InvoicePaymentModelDao>() {
                                                                                                                                             @Override
                                                                                                                                             public InvoicePaymentModelDao apply(final InvoicePayment input) {
                                                                                                                                                 return new InvoicePaymentModelDao(input);
                                                                                                                                             }
                                                                                                                                         }));

        // The test does not use the invoice callback notifier hence the empty map
        invoiceDao.createInvoice(invoiceModelDao, invoiceItemModelDaos, invoicePaymentModelDaos, isRealInvoiceWithItems, ImmutableMap.<UUID, DateTime>of(), internalCallContext);
    }

    public void createPayment(final InvoicePayment invoicePayment, final InternalCallContext internalCallContext) {
        try {
            invoicePaymentSqlDao.create(new InvoicePaymentModelDao(invoicePayment), internalCallContext);
        } catch (EntityPersistenceException e) {
            Assert.fail(e.getMessage());
        }
    }

    public void verifyInvoice(final UUID invoiceId, final double balance, final double cbaAmount, final InternalTenantContext context) throws InvoiceApiException {
        final InvoiceModelDao invoice = invoiceDao.getById(invoiceId, context);
        Assert.assertEquals(InvoiceModelDaoHelper.getBalance(invoice).doubleValue(), balance);
        Assert.assertEquals(InvoiceModelDaoHelper.getCBAAmount(invoice).doubleValue(), cbaAmount);
    }

    public void checkInvoicesEqual(final InvoiceModelDao retrievedInvoice, final Invoice invoice) {
        Assert.assertEquals(retrievedInvoice.getId(), invoice.getId());
        Assert.assertEquals(retrievedInvoice.getAccountId(), invoice.getAccountId());
        Assert.assertEquals(retrievedInvoice.getCurrency(), invoice.getCurrency());
        Assert.assertEquals(retrievedInvoice.getInvoiceDate(), invoice.getInvoiceDate());
        Assert.assertEquals(retrievedInvoice.getTargetDate(), invoice.getTargetDate());
        Assert.assertEquals(retrievedInvoice.getInvoiceItems().size(), invoice.getInvoiceItems().size());
        Assert.assertEquals(retrievedInvoice.getInvoicePayments().size(), invoice.getPayments().size());
    }

    public LocalDate buildDate(final int year, final int month, final int day) {
        return new LocalDate(year, month, day);
    }

    public BillingEvent createMockBillingEvent(@Nullable final Account account, final SubscriptionBase subscription,
                                               final DateTime effectiveDate,
                                               final Plan plan, final PlanPhase planPhase,
                                               @Nullable final BigDecimal fixedPrice, @Nullable final BigDecimal recurringPrice,
                                               final Currency currency, final BillingPeriod billingPeriod,
                                               final int billCycleDayLocal,
                                               final BillingModeType billingModeType, final String description,
                                               final long totalOrdering,
                                               final SubscriptionBaseTransitionType type) {
        return new BillingEvent() {
            @Override
            public Account getAccount() {
                return account;
            }

            @Override
            public int getBillCycleDayLocal() {
                return billCycleDayLocal;
            }

            @Override
            public SubscriptionBase getSubscription() {
                return subscription;
            }

            @Override
            public DateTime getEffectiveDate() {
                return effectiveDate;
            }

            @Override
            public PlanPhase getPlanPhase() {
                return planPhase;
            }

            @Override
            public Plan getPlan() {
                return plan;
            }

            @Override
            public BillingPeriod getBillingPeriod() {
                return billingPeriod;
            }

            @Override
            public BillingModeType getBillingMode() {
                return billingModeType;
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public BigDecimal getFixedPrice() {
                return fixedPrice;
            }

            @Override
            public BigDecimal getRecurringPrice() {
                return recurringPrice;
            }

            @Override
            public Currency getCurrency() {
                return currency;
            }

            @Override
            public SubscriptionBaseTransitionType getTransitionType() {
                return type;
            }

            @Override
            public Long getTotalOrdering() {
                return totalOrdering;
            }

            @Override
            public DateTimeZone getTimeZone() {
                return DateTimeZone.UTC;
            }

            @Override
            public int compareTo(final BillingEvent e1) {
                if (!getSubscription().getId().equals(e1.getSubscription().getId())) { // First order by subscription
                    return getSubscription().getId().compareTo(e1.getSubscription().getId());
                } else { // subscriptions are the same
                    if (!getEffectiveDate().equals(e1.getEffectiveDate())) { // Secondly order by date
                        return getEffectiveDate().compareTo(e1.getEffectiveDate());
                    } else { // dates and subscriptions are the same
                        return getTotalOrdering().compareTo(e1.getTotalOrdering());
                    }
                }
            }
        };
    }
}