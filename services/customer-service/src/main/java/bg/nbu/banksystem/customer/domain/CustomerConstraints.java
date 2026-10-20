package bg.nbu.banksystem.customer.domain;

/**
 * Length limits shared by Customer API validation and JPA mapping.
 */
public final class CustomerConstraints {

    public static final int CUSTOMER_NUMBER_MAX_LENGTH = 20;
    public static final int CUSTOMER_TYPE_MAX_LENGTH = 20;
    public static final int STATUS_MAX_LENGTH = 20;

    public static final int NAME_MAX_LENGTH = 100;
    public static final int COMPANY_NAME_MAX_LENGTH = 200;

    public static final int EMAIL_MAX_LENGTH = 254;
    public static final int PHONE_MAX_LENGTH = 30;

    public static final int EGN_LENGTH = 10;
    public static final int EIK_LENGTH = 9;

    public static final int COUNTRY_CODE_LENGTH = 2;
    public static final int CITY_MAX_LENGTH = 100;
    public static final int POSTAL_CODE_MAX_LENGTH = 20;
    public static final int ADDRESS_LINE_MAX_LENGTH = 200;

    public static final int REPRESENTATIVE_ROLE_MAX_LENGTH = 40;

    private CustomerConstraints() {
    }
}