/*
 * Copyright (c) 2014 Swen Walkowski.
 * All rights reserved. Originator: Swen Walkowski.
 * Get more information about CardDAVSyncOutlook at https://github.com/somedevelopment/CardDAVSyncOutlook/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package contact;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.ImageType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Anniversary;
import ezvcard.property.Birthday;
import ezvcard.property.Email;
import ezvcard.property.Note;
import ezvcard.property.Organization;
import ezvcard.property.Photo;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import ezvcard.property.Title;
import ezvcard.property.Uid;
import ezvcard.property.Url;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import utilities.LegacyCorrectionUtilities;

public class Contact {

    public enum Status {

        CHANGED,
        UNCHANGED,
        DELETE,
        NEW,
        READIN,
        UIDADDED
    }

    private VCard vcard = null;
    private String strPathToContactPicture = null;
    private String strUid = null;
    // unique ID of an item in outlook. Changes when item is moved or recreated from backup
    private String strEntryID = null;
    private String strFileOnDavServer = null;
    private Status statusContact = null;
    private Date dateLastModificationTme = null;

    /** Constructor Section */

    /**
     * Create a copy of an existing contact.
     */
    public Contact(Contact toCopyContact, Contact.Status state) {
        this(toCopyContact, state, toCopyContact.getUid());
    }

    /**
     * Create a copy of an existing contact with a new UID.
     */
    public Contact(Contact toCopyContact, Contact.Status state, String uid) {
        this.statusContact = state;

        this.vcard = Ezvcard.parse(toCopyContact.getContactAsString()).first();
        this.strUid = uid;
        this.strFileOnDavServer = toCopyContact.getFileOnDavServer();
        this.strEntryID = toCopyContact.getEntryID();
        this.strPathToContactPicture = toCopyContact.getPathToContactPicture();
        this.dateLastModificationTme = toCopyContact.getLastModificationTime();
    }

    /**
     * Create contact from WebDAV.
     */
    public Contact(String strVCardData, String strFileOnDavServer, String strWorkingDir) {
        this.statusContact = Status.READIN;

        this.vcard = Ezvcard.parse(strVCardData).first();
        this.strUid = this.vcard.getUid().getValue();
        this.strFileOnDavServer = strFileOnDavServer;
        this.strEntryID = null;

        this.getContactAsString();

        if (this.vcard.getPhotos().size() > 0) {
            Photo photo = this.vcard.getPhotos().get(0);
            byte[] photoData = photo.getData();
            // data can be null
            if (photoData != null) {
                this.strPathToContactPicture = strWorkingDir + Math.random() + ".jpg";
                File tmpFile = new File(strPathToContactPicture);
                try {
                    FileUtils.writeByteArrayToFile(tmpFile, photoData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        this.dateLastModificationTme = this.vcard.getRevision().getValue();
    }

    /**
     * Create contact from Outlook.
     */
    public Contact(String strUid, String strEntryID, String strTitle, String strFirstName, String strMiddleName, String strLastName, String strSuffix,
            String strCompanyName, String strJobTitle, String strEmail1Address, String strEmail2Address, String strEmail3Address,
            String strWebPage, String strMobileTelephoneNumber, String strAssistantTelephoneNumber, String strCallbackTelephoneNumber,
            String strCarTelephoneNumber, String strCompanyMainTelephoneNumber, String strOtherTelephoneNumber,
            String strPrimaryTelephoneNumber, String strRadioTelephoneNumber, String strTTYTDDTelephoneNumber, String strBusinessTelephoneNumber, String strBusiness2TelephoneNumber,
            String strBusinessFaxNumber, String strHomeTelephoneNumber, String strHome2TelephoneNumber, String strHomeFaxNumber, String strHomeAddressCity,
            String strHomeAddressCountry, String strHomeAddressPostalCode, String strHomeAddressState, String strHomeAddressStreet,
            String strBusinessAddressCity, String strBusinessAddressCountry, String strBusinessAddressPostalCode, String strBusinessAddressState,
            String strBusinessAddressStreet, String strBody, Calendar calBirthday, Calendar calAnniversary, String strPathToTmpPicture, String strLastModificationTime) {

        this.statusContact = Status.READIN;

        this.vcard = new VCard();

        //Legacy Correction with regards to the UID string which is included in the Body/Note field
        if (strUid.isEmpty()) {
            strUid = LegacyCorrectionUtilities.getBodyUID(strBody);
            this.statusContact = Status.UIDADDED;
        }

        if (strUid.isEmpty()) {
            this.vcard.setUid(Uid.random());
            this.statusContact = Status.UIDADDED;
        } else {
            this.vcard.setUid(new Uid(strUid.trim()));
        }
        this.strUid = this.vcard.getUid().getValue();

        this.strFileOnDavServer = null;

        if (strEntryID.length() > 0) {
            this.strEntryID = strEntryID;
        }

        if (strTitle.length() > 0 || strFirstName.length() > 0 || strMiddleName.length() > 0
                || strLastName.length() > 0 || strSuffix.length() > 0) {
            StructuredName sn = new StructuredName();
            sn.addPrefix(strTitle);
            sn.setGiven(strFirstName);
            sn.addAdditional(strMiddleName);
            sn.setFamily(strLastName);
            sn.addSuffix(strSuffix);
            this.vcard.setStructuredName(sn);
        }

        if (strCompanyName.length() > 0) {
            Organization org = new Organization();
            org.addValue(strCompanyName);
            this.vcard.setOrganization(org);
        }

        if (strJobTitle.length() > 0) {
            this.vcard.addTitle(new Title(strJobTitle));
        }

        if (strEmail1Address.length() > 0) {
            Email email = new Email(strEmail1Address);
            email.addType(EmailType.HOME);
            this.vcard.addEmail(email);
        }

        if (strEmail2Address.length() > 0) {
            Email email = new Email(strEmail2Address);
            email.addType(EmailType.WORK);
            this.vcard.addEmail(email);
        }

        if (strEmail3Address.length() > 0) {
            Email email = new Email(strEmail3Address);
            email.addType(EmailType.INTERNET);
            this.vcard.addEmail(email);
        }

        if (strWebPage.length() > 0) {
            this.vcard.addUrl(new Url(strWebPage));
        }

        if (strMobileTelephoneNumber.length() > 0) {
            Telephone tel = new Telephone(strMobileTelephoneNumber);
            tel.addType(TelephoneType.CELL);
            this.vcard.addTelephoneNumber(tel);
        }

        if (strAssistantTelephoneNumber.length() > 0) {
            Telephone tel = new Telephone(strAssistantTelephoneNumber);
            tel.addType(TelephoneType.BBS);
            this.vcard.addTelephoneNumber(tel);
        }

        if (strCallbackTelephoneNumber.length() > 0) {
            Telephone tel = new Telephone(strCallbackTelephoneNumber);
            tel.addType(TelephoneType.MODEM);
            this.vcard.addTelephoneNumber(tel);
        }

        if (strCarTelephoneNumber.length() > 0) {
            Telephone tel = new Telephone(strCarTelephoneNumber);
            tel.addType(TelephoneType.PAGER);
            this.vcard.addTelephoneNumber(tel);
        }

        if (strCompanyMainTelephoneNumber.length() > 0) {
            Telephone tel = new Telephone(strCompanyMainTelephoneNumber);
            tel.addType(TelephoneType.PCS);
            this.vcard.addTelephoneNumber(tel);
        }

        if (strOtherTelephoneNumber.length() > 0) {
            Telephone tel = new Telephone(strOtherTelephoneNumber);
            tel.addType(TelephoneType.PREF);
            this.vcard.addTelephoneNumber(tel);
        }

        if (strPrimaryTelephoneNumber.length() > 0) {
            Telephone tel = new Telephone(strPrimaryTelephoneNumber);
            tel.addType(TelephoneType.VIDEO);
            this.vcard.addTelephoneNumber(tel);
        }

        if (strRadioTelephoneNumber.length() > 0) {
            Telephone tel = new Telephone(strRadioTelephoneNumber);
            tel.addType(TelephoneType.VOICE);
            this.vcard.addTelephoneNumber(tel);
        }

        if (strTTYTDDTelephoneNumber.length() > 0) {
            Telephone tel = new Telephone(strTTYTDDTelephoneNumber);
            tel.addType(TelephoneType.TEXT);
            this.vcard.addTelephoneNumber(tel);
        }

        if (strBusinessTelephoneNumber.length() > 0) {
            Telephone tel = new Telephone(strBusinessTelephoneNumber);
            tel.addType(TelephoneType.WORK);
            this.vcard.addTelephoneNumber(tel);
        }

        if (strBusiness2TelephoneNumber.length() > 0) {
            Telephone tel = new Telephone(strBusiness2TelephoneNumber);
            tel.addType(TelephoneType.CAR);
            this.vcard.addTelephoneNumber(tel);
        }

        if (strBusinessFaxNumber.length() > 0) {
            Telephone tel = new Telephone(strBusinessFaxNumber);
            tel.addType(TelephoneType.FAX);
            this.vcard.addTelephoneNumber(tel);
        }

        if (strHomeTelephoneNumber.length() > 0) {
            Telephone tel = new Telephone(strHomeTelephoneNumber);
            tel.addType(TelephoneType.HOME);
            this.vcard.addTelephoneNumber(tel);
        }

        if (strHome2TelephoneNumber.length() > 0) {
            Telephone tel = new Telephone(strHome2TelephoneNumber);
            tel.addType(TelephoneType.ISDN);
            this.vcard.addTelephoneNumber(tel);
        }

        if (strHomeFaxNumber.length() > 0) {
            Telephone tel = new Telephone(strHomeFaxNumber);
            tel.addType(TelephoneType.MSG);
            this.vcard.addTelephoneNumber(tel);
        }

        if (strHomeAddressStreet.length() > 0 || strHomeAddressCity.length() > 0 || strHomeAddressState.length() > 0
                || strHomeAddressPostalCode.length() > 0 || strHomeAddressCountry.length() > 0) {
            Address adr = new Address();
            adr.setStreetAddress(strHomeAddressStreet);
            adr.setLocality(strHomeAddressCity);
            adr.setRegion(strHomeAddressState);
            adr.setPostalCode(strHomeAddressPostalCode);
            adr.setCountry(strHomeAddressCountry);
            adr.addType(AddressType.HOME);
            this.vcard.addAddress(adr);
        }

        if (strBusinessAddressStreet.length() > 0 || strBusinessAddressCity.length() > 0 || strBusinessAddressState.length() > 0
                || strBusinessAddressPostalCode.length() > 0 || strBusinessAddressCountry.length() > 0) {
            Address adr = new Address();
            adr.setStreetAddress(strBusinessAddressStreet);
            adr.setLocality(strBusinessAddressCity);
            adr.setRegion(strBusinessAddressState);
            adr.setPostalCode(strBusinessAddressPostalCode);
            adr.setCountry(strBusinessAddressCountry);
            adr.addType(AddressType.WORK);
            this.vcard.addAddress(adr);
        }

        if (strBody.length() > 0) {
            Note note = new Note(strBody);
            this.vcard.addNote(note);
        }

        if (calBirthday != null) {
            this.vcard.setBirthday(new Birthday(calBirthday.getTime()));
        }

        if (calAnniversary != null) {
            this.vcard.setAnniversary(new Anniversary(calAnniversary.getTime()));
        }

        if (strPathToTmpPicture != null) {
            this.strPathToContactPicture = strPathToTmpPicture;

            File tmpFile = new File(this.strPathToContactPicture);
            if (tmpFile.exists()) {
                try {
                    this.vcard.addPhoto(new Photo(Files.readAllBytes(tmpFile.toPath()), ImageType.JPEG));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
            this.dateLastModificationTme = sdf.parse(strLastModificationTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Private Section
     */
    public String internationalNumber(String strPhoneNumber, String strDefaultRegion) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        PhoneNumber phoneNumber = new PhoneNumber();

        try {
            phoneNumber = phoneUtil.parse(strPhoneNumber, strDefaultRegion);
        } catch (NumberParseException e) {
            e.printStackTrace();
        }
        strPhoneNumber = phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
        main.Status.print("Changed to international number format: " + strPhoneNumber);

        return strPhoneNumber;
    }

    private Boolean comparePictures(String strPathToComparePicture) {
        BufferedImage imageA;
        BufferedImage imageB;

        File fileImageA = new File(this.strPathToContactPicture);
        File fileImageB = new File(strPathToComparePicture);

        if (!fileImageA.exists() || !fileImageB.exists()) {
            return false;
        }

        try {
            imageA = ImageIO.read(new File(this.strPathToContactPicture));
            imageB = ImageIO.read(new File(strPathToComparePicture));

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        int[] pixelOfA = new int[imageA.getWidth() * imageA.getHeight()];
        imageA.getData().getPixel(0, 0, pixelOfA);

        int[] pixelOfB = new int[imageB.getWidth() * imageB.getHeight()];
        imageB.getData().getPixel(0, 0, pixelOfB);

        return Arrays.equals(pixelOfA, pixelOfB);
    }

    /**
     * Public Section
     */
    public void correctNumbers(String strDefaultRegion) {
        for (Telephone number : this.vcard.getTelephoneNumbers()) {
            number.setText(internationalNumber(number.getText(), strDefaultRegion));
        }
    }

    public void deleteTmpContactPictureFile() {
        if (this.strPathToContactPicture != null) {
            File tmpFile = new File(this.strPathToContactPicture);

            if (tmpFile.exists()) {
                tmpFile.delete();
            }
        }
    }

    public boolean equalTo(Contact toCompareContact) {

        if (this.getAnniversary() != null) {
            if (toCompareContact.getAnniversary() != null) {
                if (!this.getAnniversary().equals(toCompareContact.getAnniversary())) {
                    //mainPackage.Status.printStatusToConsole("getAnniversary");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getAnniversary");
                return false;
            }
        } else if (toCompareContact.getAnniversary() != null) {
            //mainPackage.Status.printStatusToConsole("getAnniversary");
            return false;
        }

        if (this.getBirthday() != null) {
            if (toCompareContact.getBirthday() != null) {
                if (!this.getBirthday().equals(toCompareContact.getBirthday())) {
                    //mainPackage.Status.printStatusToConsole("getBirthday");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getBirthday");
                return false;
            }
        } else if (toCompareContact.getBirthday() != null) {
            //mainPackage.Status.printStatusToConsole("getBirthday");
            return false;
        }

        if (this.strPathToContactPicture != null) {
            if (toCompareContact.getPathToContactPicture() != null) {
                if (!this.comparePictures(toCompareContact.getPathToContactPicture())) {
                    //mainPackage.Status.printStatusToConsole("getPathToContactPicture");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getPathToContactPicture");
                return false;
            }
        } else if (toCompareContact.getPathToContactPicture() != null) {
            //mainPackage.Status.printStatusToConsole("getPathToContactPicture");
            return false;
        }

        if (this.getTitle() != null) {
            if (toCompareContact.getTitle() != null) {
                if (!this.getTitle().equals(toCompareContact.getTitle())) {
                    //mainPackage.Status.printStatusToConsole("getTitle");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getTitle");
                return false;
            }
        } else if (toCompareContact.getTitle() != null) {
            //mainPackage.Status.printStatusToConsole("getTitle");
            return false;
        }

        if (this.getFirstName() != null) {
            if (toCompareContact.getFirstName() != null) {
                if (!this.getFirstName().equals(toCompareContact.getFirstName())) {
                    //mainPackage.Status.printStatusToConsole("getFirstName");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getFirstName");
                return false;
            }
        } else if (toCompareContact.getFirstName() != null) {
            //mainPackage.Status.printStatusToConsole("getFirstName");
            return false;
        }

        if (this.getMiddleName() != null) {
            if (toCompareContact.getMiddleName() != null) {
                if (!this.getMiddleName().equals(toCompareContact.getMiddleName())) {
                    //mainPackage.Status.printStatusToConsole("getMiddleName");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getMiddleName");
                return false;
            }
        } else if (toCompareContact.getMiddleName() != null) {
            //mainPackage.Status.printStatusToConsole("getMiddleName");
            return false;
        }

        if (this.getLastName() != null) {
            if (toCompareContact.getLastName() != null) {
                if (!this.getLastName().equals(toCompareContact.getLastName())) {
                    //mainPackage.Status.printStatusToConsole("getLastName");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getLastName");
                return false;
            }
        } else if (toCompareContact.getLastName() != null) {
            //mainPackage.Status.printStatusToConsole("getLastName");
            return false;
        }

        if (this.getCompanyName() != null) {
            if (toCompareContact.getCompanyName() != null) {
                if (!this.getCompanyName().equals(toCompareContact.getCompanyName())) {
                    //mainPackage.Status.printStatusToConsole("getCompanyName");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getCompanyName");
                return false;
            }
        } else if (toCompareContact.getCompanyName() != null) {
            //mainPackage.Status.printStatusToConsole("getCompanyName");
            return false;
        }

        if (this.getSuffix() != null) {
            if (toCompareContact.getSuffix() != null) {
                if (!this.getSuffix().equals(toCompareContact.getSuffix())) {
                    //mainPackage.Status.printStatusToConsole("getSuffix");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getSuffix");
                return false;
            }
        } else if (toCompareContact.getSuffix() != null) {
            //mainPackage.Status.printStatusToConsole("getSuffix");
            return false;
        }

        if (this.getJobTitle() != null) {
            if (toCompareContact.getJobTitle() != null) {
                if (!this.getJobTitle().equals(toCompareContact.getJobTitle())) {
                    //mainPackage.Status.printStatusToConsole("getJobTitle");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getJobTitle");
                return false;
            }
        } else if (toCompareContact.getJobTitle() != null) {
            //mainPackage.Status.printStatusToConsole("getJobTitle");
            return false;
        }

        if (this.getEmail1Address() != null) {
            if (toCompareContact.getEmail1Address() != null) {
                if (!this.getEmail1Address().equals(toCompareContact.getEmail1Address())) {
                    //mainPackage.Status.printStatusToConsole("getEmail1Address");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getEmail1Address");
                return false;
            }
        } else if (toCompareContact.getEmail1Address() != null) {
            //mainPackage.Status.printStatusToConsole("getEmail1Address");
            return false;
        }

        if (this.getEmail2Address() != null) {
            if (toCompareContact.getEmail2Address() != null) {
                if (!this.getEmail2Address().equals(toCompareContact.getEmail2Address())) {
                    //mainPackage.Status.printStatusToConsole("getEmail2Address");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getEmail2Address");
                return false;
            }
        } else if (toCompareContact.getEmail2Address() != null) {
            //mainPackage.Status.printStatusToConsole("getEmail2Address");
            return false;
        }

        if (this.getEmail3Address() != null) {
            if (toCompareContact.getEmail3Address() != null) {
                if (!this.getEmail3Address().equals(toCompareContact.getEmail3Address())) {
                    //mainPackage.Status.printStatusToConsole("getEmail3Address");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getEmail3Address");
                return false;
            }
        } else if (toCompareContact.getEmail3Address() != null) {
            //mainPackage.Status.printStatusToConsole("getEmail3Address");
            return false;
        }

        if (this.getWebPage() != null) {
            if (toCompareContact.getWebPage() != null) {
                if (!this.getWebPage().equals(toCompareContact.getWebPage())) {
                    //mainPackage.Status.printStatusToConsole("getWebPage");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getWebPage");
                return false;
            }
        } else if (toCompareContact.getWebPage() != null) {
            //mainPackage.Status.printStatusToConsole("getWebPage");
            return false;
        }

        if (this.getMobileTelephoneNumber() != null) {
            if (toCompareContact.getMobileTelephoneNumber() != null) {
                if (!this.getMobileTelephoneNumber().equals(toCompareContact.getMobileTelephoneNumber())) {
                    //mainPackage.Status.printStatusToConsole("getMobileTelephoneNumber");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getMobileTelephoneNumber");
                return false;
            }
        } else if (toCompareContact.getMobileTelephoneNumber() != null) {
            //mainPackage.Status.printStatusToConsole("getMobileTelephoneNumber");
            return false;
        }

        if (this.getBusinessTelephoneNumber() != null) {
            if (toCompareContact.getBusinessTelephoneNumber() != null) {
                if (!this.getBusinessTelephoneNumber().equals(toCompareContact.getBusinessTelephoneNumber())) {
                    //mainPackage.Status.printStatusToConsole("getBusinessTelephoneNumber");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getBusinessTelephoneNumber");
                return false;
            }
        } else if (toCompareContact.getBusinessTelephoneNumber() != null) {
            //mainPackage.Status.printStatusToConsole("getBusinessTelephoneNumber");
            return false;
        }

        if (this.getBusiness2TelephoneNumber() != null) {
            if (toCompareContact.getBusiness2TelephoneNumber() != null) {
                if (!this.getBusiness2TelephoneNumber().equals(toCompareContact.getBusiness2TelephoneNumber())) {
                    //mainPackage.Status.printStatusToConsole("getBusiness2TelephoneNumber");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getBusiness2TelephoneNumber");
                return false;
            }
        } else if (toCompareContact.getBusiness2TelephoneNumber() != null) {
            //mainPackage.Status.printStatusToConsole("getBusiness2TelephoneNumber");
            return false;
        }

        if (this.getHomeTelephoneNumber() != null) {
            if (toCompareContact.getHomeTelephoneNumber() != null) {
                if (!this.getHomeTelephoneNumber().equals(toCompareContact.getHomeTelephoneNumber())) {
                    //mainPackage.Status.printStatusToConsole("getHomeTelephoneNumber");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getHomeTelephoneNumber");
                return false;
            }
        } else if (toCompareContact.getHomeTelephoneNumber() != null) {
            //mainPackage.Status.printStatusToConsole("getHomeTelephoneNumber");
            return false;
        }

        if (this.getBusinessFaxNumber() != null) {
            if (toCompareContact.getBusinessFaxNumber() != null) {
                if (!this.getBusinessFaxNumber().equals(toCompareContact.getBusinessFaxNumber())) {
                    //mainPackage.Status.printStatusToConsole("getBusinessFaxNumber");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getBusinessFaxNumber");
                return false;
            }
        } else if (toCompareContact.getBusinessFaxNumber() != null) {
            //mainPackage.Status.printStatusToConsole("getBusinessFaxNumber");
            return false;
        }

        if (this.getHome2TelephoneNumber() != null) {
            if (toCompareContact.getHome2TelephoneNumber() != null) {
                if (!this.getHome2TelephoneNumber().equals(toCompareContact.getHome2TelephoneNumber())) {
                    //mainPackage.Status.printStatusToConsole("getHome2TelephoneNumber");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getHome2TelephoneNumber");
                return false;
            }
        } else if (toCompareContact.getHome2TelephoneNumber() != null) {
            //mainPackage.Status.printStatusToConsole("getHome2TelephoneNumber");
            return false;
        }

        if (this.getHomeFaxNumber() != null) {
            if (toCompareContact.getHomeFaxNumber() != null) {
                if (!this.getHomeFaxNumber().equals(toCompareContact.getHomeFaxNumber())) {
                    //mainPackage.Status.printStatusToConsole("getHomeFaxNumber");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getHomeFaxNumber");
                return false;
            }
        } else if (toCompareContact.getHomeFaxNumber() != null) {
            //mainPackage.Status.printStatusToConsole("getHomeFaxNumber");
            return false;
        }

        if (this.getHome2TelephoneNumber() != null) {
            if (toCompareContact.getHome2TelephoneNumber() != null) {
                if (!this.getHome2TelephoneNumber().equals(toCompareContact.getHome2TelephoneNumber())) {
                    //mainPackage.Status.printStatusToConsole("getHome2TelephoneNumber");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getHome2TelephoneNumber");
                return false;
            }
        } else if (toCompareContact.getHome2TelephoneNumber() != null) {
            //mainPackage.Status.printStatusToConsole("getHome2TelephoneNumber");
            return false;
        }

        if (this.getHomeAddressCity() != null) {
            if (toCompareContact.getHomeAddressCity() != null) {
                if (!this.getHomeAddressCity().equals(toCompareContact.getHomeAddressCity())) {
                    //mainPackage.Status.printStatusToConsole("getHomeAddressCity");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getHomeAddressCity");
                return false;
            }
        } else if (toCompareContact.getHomeAddressCity() != null) {
            //mainPackage.Status.printStatusToConsole("getHomeAddressCity");
            return false;
        }

        if (this.getHomeAddressCountry() != null) {
            if (toCompareContact.getHomeAddressCountry() != null) {
                if (!this.getHomeAddressCountry().equals(toCompareContact.getHomeAddressCountry())) {
                    //mainPackage.Status.printStatusToConsole("getHomeAddressCountry");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getHomeAddressCountry");
                return false;
            }
        } else if (toCompareContact.getHomeAddressCountry() != null) {
            //mainPackage.Status.printStatusToConsole("getHomeAddressCountry");
            return false;
        }

        if (this.getHomeAddressPostalCode() != null) {
            if (toCompareContact.getHomeAddressPostalCode() != null) {
                if (!this.getHomeAddressPostalCode().equals(toCompareContact.getHomeAddressPostalCode())) {
                    //mainPackage.Status.printStatusToConsole("getHomeAddressPostalCode");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getHomeAddressPostalCode");
                return false;
            }
        } else if (toCompareContact.getHomeAddressPostalCode() != null) {
            //mainPackage.Status.printStatusToConsole("getHomeAddressPostalCode");
            return false;
        }

        if (this.getHomeAddressState() != null) {
            if (toCompareContact.getHomeAddressState() != null) {
                if (!this.getHomeAddressState().equals(toCompareContact.getHomeAddressState())) {
                    //mainPackage.Status.printStatusToConsole("getHomeAddressState");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getHomeAddressState");
                return false;
            }
        } else if (toCompareContact.getHomeAddressState() != null) {
            //mainPackage.Status.printStatusToConsole("getHomeAddressState");
            return false;
        }

        if (this.getHomeAddressStreet() != null) {
            if (toCompareContact.getHomeAddressStreet() != null) {
                if (!this.getHomeAddressStreet().equals(toCompareContact.getHomeAddressStreet())) {
                    //mainPackage.Status.printStatusToConsole("getHomeAddressStreet");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getHomeAddressStreet");
                return false;
            }
        } else if (toCompareContact.getHomeAddressStreet() != null) {
            //mainPackage.Status.printStatusToConsole("getHomeAddressStreet");
            return false;
        }

        if (this.getBusinessAddressCity() != null) {
            if (toCompareContact.getBusinessAddressCity() != null) {
                if (!this.getBusinessAddressCity().equals(toCompareContact.getBusinessAddressCity())) {
                    //mainPackage.Status.printStatusToConsole("getBusinessAddressCity");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getBusinessAddressCity");
                return false;
            }
        } else if (toCompareContact.getBusinessAddressCity() != null) {
            //mainPackage.Status.printStatusToConsole("getBusinessAddressCity");
            return false;
        }

        if (this.getBusinessAddressCountry() != null) {
            if (toCompareContact.getBusinessAddressCountry() != null) {
                if (!this.getBusinessAddressCountry().equals(toCompareContact.getBusinessAddressCountry())) {
                    //mainPackage.Status.printStatusToConsole("getBusinessAddressCountry");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getBusinessAddressCountry");
                return false;
            }
        } else if (toCompareContact.getBusinessAddressCountry() != null) {
            //mainPackage.Status.printStatusToConsole("getBusinessAddressCountry");
            return false;
        }

        if (this.getBusinessAddressPostalCode() != null) {
            if (toCompareContact.getBusinessAddressPostalCode() != null) {
                if (!this.getBusinessAddressPostalCode().equals(toCompareContact.getBusinessAddressPostalCode())) {
                    //mainPackage.Status.printStatusToConsole("getBusinessAddressPostalCode");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getBusinessAddressPostalCode");
                return false;
            }
        } else if (toCompareContact.getBusinessAddressPostalCode() != null) {
            //mainPackage.Status.printStatusToConsole("getBusinessAddressPostalCode");
            return false;
        }

        if (this.getBusinessAddressState() != null) {
            if (toCompareContact.getBusinessAddressState() != null) {
                if (!this.getBusinessAddressState().equals(toCompareContact.getBusinessAddressState())) {
                    //mainPackage.Status.printStatusToConsole("getBusinessAddressState");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getBusinessAddressState");
                return false;
            }
        } else if (toCompareContact.getBusinessAddressState() != null) {
            //mainPackage.Status.printStatusToConsole("getBusinessAddressState");
            return false;
        }

        if (this.getBusinessAddressStreet() != null) {
            if (toCompareContact.getBusinessAddressStreet() != null) {
                if (!this.getBusinessAddressStreet().equals(toCompareContact.getBusinessAddressStreet())) {
                    //mainPackage.Status.printStatusToConsole("getBusinessAddressStreet");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getBusinessAddressStreet");
                return false;
            }
        } else if (toCompareContact.getBusinessAddressStreet() != null) {
            //mainPackage.Status.printStatusToConsole("getBusinessAddressStreet");
            return false;
        }
        if (this.getBody() != null) {
            if (toCompareContact.getBody() != null) {
                if (!this.getBody().equals(toCompareContact.getBody())) {
                    //mainPackage.Status.printStatusToConsole("getBody");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getBody");
                return false;
            }
        } else if (toCompareContact.getBody() != null) {
            //mainPackage.Status.printStatusToConsole("getBody");
            return false;
        }

        if (this.getAssistantTelephoneNumber() != null) {
            if (toCompareContact.getAssistantTelephoneNumber() != null) {
                if (!this.getAssistantTelephoneNumber().equals(toCompareContact.getAssistantTelephoneNumber())) {
                    //mainPackage.Status.printStatusToConsole("getAssistantTelephoneNumber");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getAssistantTelephoneNumber");
                return false;
            }
        } else if (toCompareContact.getAssistantTelephoneNumber() != null) {
            //mainPackage.Status.printStatusToConsole("getAssistantTelephoneNumber");
            return false;
        }

        if (this.getCallbackTelephoneNumber() != null) {
            if (toCompareContact.getCallbackTelephoneNumber() != null) {
                if (!this.getCallbackTelephoneNumber().equals(toCompareContact.getCallbackTelephoneNumber())) {
                    //mainPackage.Status.printStatusToConsole("getCallbackTelephoneNumber");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getCallbackTelephoneNumber");
                return false;
            }
        } else if (toCompareContact.getCallbackTelephoneNumber() != null) {
            //mainPackage.Status.printStatusToConsole("getCallbackTelephoneNumber");
            return false;
        }

        if (this.getCarTelephoneNumber() != null) {
            if (toCompareContact.getCarTelephoneNumber() != null) {
                if (!this.getCarTelephoneNumber().equals(toCompareContact.getCarTelephoneNumber())) {
                    //mainPackage.Status.printStatusToConsole("getCarTelephoneNumber");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getCarTelephoneNumber");
                return false;
            }
        } else if (toCompareContact.getCarTelephoneNumber() != null) {
            //mainPackage.Status.printStatusToConsole("getCarTelephoneNumber");
            return false;
        }

        if (this.getCompanyMainTelephoneNumber() != null) {
            if (toCompareContact.getCompanyMainTelephoneNumber() != null) {
                if (!this.getCompanyMainTelephoneNumber().equals(toCompareContact.getCompanyMainTelephoneNumber())) {
                    //mainPackage.Status.printStatusToConsole("getCompanyMainTelephoneNumber");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getCompanyMainTelephoneNumber");
                return false;
            }
        } else if (toCompareContact.getCompanyMainTelephoneNumber() != null) {
            //mainPackage.Status.printStatusToConsole("getCompanyMainTelephoneNumber");
            return false;
        }

        if (this.getOtherTelephoneNumber() != null) {
            if (toCompareContact.getOtherTelephoneNumber() != null) {
                if (!this.getOtherTelephoneNumber().equals(toCompareContact.getOtherTelephoneNumber())) {
                    //mainPackage.Status.printStatusToConsole("getOtherTelephoneNumber");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getOtherTelephoneNumber");
                return false;
            }
        } else if (toCompareContact.getOtherTelephoneNumber() != null) {
            //mainPackage.Status.printStatusToConsole("getOtherTelephoneNumber");
            return false;
        }

        if (this.getPrimaryTelephoneNumber() != null) {
            if (toCompareContact.getPrimaryTelephoneNumber() != null) {
                if (!this.getPrimaryTelephoneNumber().equals(toCompareContact.getPrimaryTelephoneNumber())) {
                    //mainPackage.Status.printStatusToConsole("getPrimaryTelephoneNumber");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getPrimaryTelephoneNumber");
                return false;
            }
        } else if (toCompareContact.getPrimaryTelephoneNumber() != null) {
            //mainPackage.Status.printStatusToConsole("getPrimaryTelephoneNumber");
            return false;
        }

        if (this.getRadioTelephoneNumber() != null) {
            if (toCompareContact.getRadioTelephoneNumber() != null) {
                if (!this.getRadioTelephoneNumber().equals(toCompareContact.getRadioTelephoneNumber())) {
                    //mainPackage.Status.printStatusToConsole("getRadioTelephoneNumber");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getRadioTelephoneNumber");
                return false;
            }
        } else if (toCompareContact.getRadioTelephoneNumber() != null) {
            //mainPackage.Status.printStatusToConsole("getRadioTelephoneNumber");
            return false;
        }

        if (this.getTTYTDDTelephoneNumber() != null) {
            if (toCompareContact.getTTYTDDTelephoneNumber() != null) {
                if (!this.getTTYTDDTelephoneNumber().equals(toCompareContact.getTTYTDDTelephoneNumber())) {
                    //mainPackage.Status.printStatusToConsole("getTTYTDDTelephoneNumber");
                    return false;
                }
            } else {
                //mainPackage.Status.printStatusToConsole("getTTYTDDTelephoneNumber");
                return false;
            }
        } else if (toCompareContact.getTTYTDDTelephoneNumber() != null) {
            //mainPackage.Status.printStatusToConsole("getTTYTDDTelephoneNumber");
            return false;
        }

        return true;
    }

    /**
     * Getter & Setter
     */
    public Status getStatus() {
        return this.statusContact;
    }

    public void setStatus(Contact.Status newStauts) {
        this.statusContact = newStauts;
    }

    public String getEntryID() {
        return this.strEntryID;
    }

    public void setEntryID(String strEntryID) {
        this.strEntryID = strEntryID;
    }

    public String getWebDavUriFilename() {
        return this.strFileOnDavServer;
    }

    public void setWebDavUriFilename(String strFileOnDavServer) {
        this.strFileOnDavServer = strFileOnDavServer;
    }

    public final String getContactAsString() {
        return this.vcard.write();
    }

    public String getUid() {
        return this.strUid;
    }

    public String getTitle() {
        if (!this.vcard.getStructuredNames().isEmpty()) {
            if (!this.vcard.getStructuredName().getPrefixes().isEmpty()) {
                if (this.vcard.getStructuredName().getPrefixes().get(0) != null) {
                    if (this.vcard.getStructuredName().getPrefixes().get(0).length() > 0) {
                        return this.vcard.getStructuredName().getPrefixes().get(0);
                    }
                }
            }
        }

        return null;
    }

    public String getFirstName() {
        if (!this.vcard.getStructuredNames().isEmpty()) {
            if (this.vcard.getStructuredName().getGiven() != null) {
                if (this.vcard.getStructuredName().getGiven().length() > 0) {
                    return this.vcard.getStructuredName().getGiven();
                }
            }
        }

        return null;
    }

    public String getMiddleName() {
        if (!this.vcard.getStructuredNames().isEmpty()) {
            if (!this.vcard.getStructuredName().getAdditional().isEmpty()) {
                if (this.vcard.getStructuredName().getAdditional().get(0) != null) {
                    if (this.vcard.getStructuredName().getAdditional().get(0).length() > 0) {
                        return this.vcard.getStructuredName().getAdditional().get(0);
                    }
                }
            }
        }

        return null;
    }

    public String getLastName() {
        if (!this.vcard.getStructuredNames().isEmpty()) {
            if (this.vcard.getStructuredName().getFamily() != null) {
                if (this.vcard.getStructuredName().getFamily().length() > 0) {
                    return this.vcard.getStructuredName().getFamily();
                }
            }
        }

        return null;
    }

    public String getSuffix() {
        if (!this.vcard.getStructuredNames().isEmpty()) {
            if (!this.vcard.getStructuredName().getSuffixes().isEmpty()) {
                if (this.vcard.getStructuredName().getSuffixes().get(0) != null) {
                    if (this.vcard.getStructuredName().getSuffixes().get(0).length() > 0) {
                        return this.vcard.getStructuredName().getSuffixes().get(0);
                    }
                }
            }
        }

        return null;
    }

    public String getCompanyName() {
        Iterator<Organization> iter = this.vcard.getOrganizations().iterator();
        while (iter.hasNext()) {
            Organization currentOrg = iter.next();

            Iterator<String> iterOrg = currentOrg.getValues().iterator();
            while (iterOrg.hasNext()) {

                return iterOrg.next();
            }
        }

        return null;
    }

    public String getJobTitle() {
        if (!this.vcard.getTitles().isEmpty()) {
            if (this.vcard.getTitles().get(0).getValue() != null) {
                if (this.vcard.getTitles().get(0).getValue().length() > 0) {
                    return this.vcard.getTitles().get(0).getValue();
                }
            }
        }

        return null;
    }

    public String getEmail1Address() {
        Iterator<Email> iter = (Iterator<Email>) this.vcard.getEmails().iterator();

        while (iter.hasNext()) {
            Email currentEmail = iter.next();
            if (currentEmail.getTypes().contains(EmailType.HOME)) {
                if (currentEmail.getValue().length() > 0) {
                    return currentEmail.getValue();
                }
            }
        }

        return null;
    }

    public String getEmail2Address() {
        Iterator<Email> iter = (Iterator<Email>) this.vcard.getEmails().iterator();

        while (iter.hasNext()) {
            Email currentEmail = iter.next();
            if (currentEmail.getTypes().contains(EmailType.WORK)) {
                if (currentEmail.getValue().length() > 0) {
                    return currentEmail.getValue();
                }
            }
        }

        return null;
    }

    public String getEmail3Address() {
        Iterator<Email> iter = (Iterator<Email>) this.vcard.getEmails().iterator();

        while (iter.hasNext()) {
            Email currentEmail = iter.next();
            if (currentEmail.getTypes().contains(EmailType.INTERNET)) {
                if (currentEmail.getValue().length() > 0) {
                    return currentEmail.getValue();
                }
            }
        }

        return null;
    }

    public String getWebPage() {
        if (!this.vcard.getUrls().isEmpty()) {
            if (this.vcard.getUrls().get(0).getValue().length() > 0) {
                return this.vcard.getUrls().get(0).getValue();
            }
        }

        return null;
    }

    public String getMobileTelephoneNumber() {
        Iterator<Telephone> iter = (Iterator<Telephone>) this.vcard.getTelephoneNumbers().iterator();

        while (iter.hasNext()) {
            Telephone currentPhone = iter.next();
            if (currentPhone.getTypes().contains(TelephoneType.CELL)) {
                if (currentPhone.getText().length() > 0) {
                    return currentPhone.getText();
                }
            }
        }

        return null;
    }

    public String getBusinessTelephoneNumber() {
        Iterator<Telephone> iter = (Iterator<Telephone>) this.vcard.getTelephoneNumbers().iterator();

        while (iter.hasNext()) {
            Telephone currentPhone = iter.next();
            if (currentPhone.getTypes().contains(TelephoneType.WORK)) {
                if (currentPhone.getText().length() > 0) {
                    return currentPhone.getText();
                }
            }
        }

        return null;
    }

    public String getBusiness2TelephoneNumber() {
        Iterator<Telephone> iter = (Iterator<Telephone>) this.vcard.getTelephoneNumbers().iterator();

        while (iter.hasNext()) {
            Telephone currentPhone = iter.next();
            if (currentPhone.getTypes().contains(TelephoneType.CAR)) {
                if (currentPhone.getText().length() > 0) {
                    return currentPhone.getText();
                }
            }
        }

        return null;
    }

    public String getBusinessFaxNumber() {
        Iterator<Telephone> iter = (Iterator<Telephone>) this.vcard.getTelephoneNumbers().iterator();

        while (iter.hasNext()) {
            Telephone currentPhone = iter.next();
            if (currentPhone.getTypes().contains(TelephoneType.FAX)) {
                if (currentPhone.getText().length() > 0) {
                    return currentPhone.getText();
                }
            }
        }

        return null;
    }

    public String getHomeTelephoneNumber() {
        Iterator<Telephone> iter = (Iterator<Telephone>) this.vcard.getTelephoneNumbers().iterator();

        while (iter.hasNext()) {
            Telephone currentPhone = iter.next();
            if (currentPhone.getTypes().contains(TelephoneType.HOME)) {
                if (currentPhone.getText().length() > 0) {
                    return currentPhone.getText();
                }
            }
        }

        return null;
    }

    public String getHome2TelephoneNumber() {
        Iterator<Telephone> iter = (Iterator<Telephone>) this.vcard.getTelephoneNumbers().iterator();

        while (iter.hasNext()) {
            Telephone currentPhone = iter.next();
            if (currentPhone.getTypes().contains(TelephoneType.ISDN)) {
                if (currentPhone.getText().length() > 0) {
                    return currentPhone.getText();
                }
            }
        }

        return null;
    }

    public String getHomeFaxNumber() {
        Iterator<Telephone> iter = (Iterator<Telephone>) this.vcard.getTelephoneNumbers().iterator();

        while (iter.hasNext()) {
            Telephone currentPhone = iter.next();
            if (currentPhone.getTypes().contains(TelephoneType.MSG)) {
                if (currentPhone.getText().length() > 0) {
                    return currentPhone.getText();
                }
            }
        }

        return null;
    }

    public String getHomeAddressCity() {
        Iterator<Address> iter = (Iterator<Address>) this.vcard.getAddresses().iterator();

        while (iter.hasNext()) {
            Address currentAddress = iter.next();
            if (currentAddress.getTypes().contains(AddressType.HOME)) {
                if (currentAddress.getLocality() != null) {
                    if (currentAddress.getLocality().length() > 0) {
                        return currentAddress.getLocality();
                    }
                }
            }
        }

        return null;
    }

    public String getHomeAddressCountry() {
        Iterator<Address> iter = (Iterator<Address>) this.vcard.getAddresses().iterator();

        while (iter.hasNext()) {
            Address currentAddress = iter.next();
            if (currentAddress.getTypes().contains(AddressType.HOME)) {
                if (currentAddress.getCountry() != null) {
                    if (currentAddress.getCountry().length() > 0) {
                        return currentAddress.getCountry();
                    }
                }
            }
        }

        return null;
    }

    public String getHomeAddressPostalCode() {
        Iterator<Address> iter = (Iterator<Address>) this.vcard.getAddresses().iterator();

        while (iter.hasNext()) {
            Address currentAddress = iter.next();
            if (currentAddress.getTypes().contains(AddressType.HOME)) {
                if (currentAddress.getPostalCode() != null) {
                    if (currentAddress.getPostalCode().length() > 0) {
                        return currentAddress.getPostalCode();
                    }
                }
            }
        }

        return null;
    }

    public String getHomeAddressState() {
        Iterator<Address> iter = (Iterator<Address>) this.vcard.getAddresses().iterator();

        while (iter.hasNext()) {
            Address currentAddress = iter.next();
            if (currentAddress.getTypes().contains(AddressType.HOME)) {
                if (currentAddress.getRegion() != null) {
                    if (currentAddress.getRegion().length() > 0) {
                        return currentAddress.getRegion();
                    }
                }
            }
        }

        return null;
    }

    public String getHomeAddressStreet() {
        Iterator<Address> iter = (Iterator<Address>) this.vcard.getAddresses().iterator();

        while (iter.hasNext()) {
            Address currentAddress = iter.next();
            if (currentAddress.getTypes().contains(AddressType.HOME)) {
                if (currentAddress.getStreetAddress() != null) {
                    if (currentAddress.getStreetAddress().length() > 0) {
                        return currentAddress.getStreetAddress();
                    }
                }
            }
        }

        return null;
    }

    public String getBusinessAddressCity() {
        Iterator<Address> iter = (Iterator<Address>) this.vcard.getAddresses().iterator();

        while (iter.hasNext()) {
            Address currentAddress = iter.next();
            if (currentAddress.getTypes().contains(AddressType.WORK)) {
                if (currentAddress.getLocality() != null) {
                    if (currentAddress.getLocality().length() > 0) {
                        return currentAddress.getLocality();
                    }
                }
            }
        }

        return null;
    }

    public String getBusinessAddressCountry() {
        Iterator<Address> iter = (Iterator<Address>) this.vcard.getAddresses().iterator();

        while (iter.hasNext()) {
            Address currentAddress = iter.next();
            if (currentAddress.getTypes().contains(AddressType.WORK)) {
                if (currentAddress.getCountry() != null) {
                    if (currentAddress.getCountry().length() > 0) {
                        return currentAddress.getCountry();
                    }
                }
            }
        }

        return null;
    }

    public String getBusinessAddressPostalCode() {
        Iterator<Address> iter = (Iterator<Address>) this.vcard.getAddresses().iterator();

        while (iter.hasNext()) {
            Address currentAddress = iter.next();
            if (currentAddress.getTypes().contains(AddressType.WORK)) {
                if (currentAddress.getPostalCode() != null) {
                    if (currentAddress.getPostalCode().length() > 0) {
                        return currentAddress.getPostalCode();
                    }
                }
            }
        }

        return null;
    }

    public String getBusinessAddressState() {
        Iterator<Address> iter = (Iterator<Address>) this.vcard.getAddresses().iterator();

        while (iter.hasNext()) {
            Address currentAddress = iter.next();
            if (currentAddress.getTypes().contains(AddressType.WORK)) {
                if (currentAddress.getRegion() != null) {
                    if (currentAddress.getRegion().length() > 0) {
                        return currentAddress.getRegion();
                    }
                }
            }
        }

        return null;
    }

    public String getBusinessAddressStreet() {
        Iterator<Address> iter = (Iterator<Address>) this.vcard.getAddresses().iterator();

        while (iter.hasNext()) {
            Address currentAddress = iter.next();
            if (currentAddress.getTypes().contains(AddressType.WORK)) {
                if (currentAddress.getStreetAddress() != null) {
                    if (currentAddress.getStreetAddress().length() > 0) {
                        return currentAddress.getStreetAddress();
                    }
                }
            }
        }
        return null;
    }

    public void setBody(String strBody) {
        if (!this.vcard.getNotes().isEmpty()) {
            this.vcard.getNotes().get(0).setValue(strBody);
        } else {
            this.vcard.addNote(strBody);
        }
    }

    public String getBody() {
        if (!this.vcard.getNotes().isEmpty()) {
            if (this.vcard.getNotes().get(0).getValue() != null) {
                if (this.vcard.getNotes().get(0).getValue().length() > 0) {
                    return this.vcard.getNotes().get(0).getValue();
                }
            }
        }
        return null;
    }

    public Date getBirthday() {
        if (!this.vcard.getBirthdays().isEmpty()) {
            return this.vcard.getBirthday().getDate();
        }
        return null;
    }

    public Date getAnniversary() {
        if (!this.vcard.getAnniversaries().isEmpty()) {
            return this.vcard.getAnniversary().getDate();
        }

        return null;
    }

    public String getAssistantTelephoneNumber() {
        Iterator<Telephone> iter = (Iterator<Telephone>) this.vcard.getTelephoneNumbers().iterator();

        while (iter.hasNext()) {
            Telephone currentPhone = iter.next();
            if (currentPhone.getTypes().contains(TelephoneType.BBS)) {
                if (currentPhone.getText().length() > 0) {
                    return currentPhone.getText();
                }
            }
        }

        return null;
    }

    public String getCallbackTelephoneNumber() {
        Iterator<Telephone> iter = (Iterator<Telephone>) this.vcard.getTelephoneNumbers().iterator();

        while (iter.hasNext()) {
            Telephone currentPhone = iter.next();
            if (currentPhone.getTypes().contains(TelephoneType.MODEM)) {
                if (currentPhone.getText().length() > 0) {
                    return currentPhone.getText();
                }
            }
        }

        return null;
    }

    public String getCarTelephoneNumber() {
        Iterator<Telephone> iter = (Iterator<Telephone>) this.vcard.getTelephoneNumbers().iterator();

        while (iter.hasNext()) {
            Telephone currentPhone = iter.next();
            if (currentPhone.getTypes().contains(TelephoneType.PAGER)) {
                if (currentPhone.getText().length() > 0) {
                    return currentPhone.getText();
                }
            }
        }

        return null;
    }

    public String getCompanyMainTelephoneNumber() {
        Iterator<Telephone> iter = (Iterator<Telephone>) this.vcard.getTelephoneNumbers().iterator();

        while (iter.hasNext()) {
            Telephone currentPhone = iter.next();
            if (currentPhone.getTypes().contains(TelephoneType.PCS)) {
                if (currentPhone.getText().length() > 0) {
                    return currentPhone.getText();
                }
            }
        }

        return null;
    }

    public String getOtherTelephoneNumber() {
        Iterator<Telephone> iter = (Iterator<Telephone>) this.vcard.getTelephoneNumbers().iterator();

        while (iter.hasNext()) {
            Telephone currentPhone = iter.next();
            if (currentPhone.getTypes().contains(TelephoneType.PREF)) {
                if (currentPhone.getText().length() > 0) {
                    return currentPhone.getText();
                }
            }
        }

        return null;
    }

    public String getPrimaryTelephoneNumber() {
        Iterator<Telephone> iter = (Iterator<Telephone>) this.vcard.getTelephoneNumbers().iterator();

        while (iter.hasNext()) {
            Telephone currentPhone = iter.next();
            if (currentPhone.getTypes().contains(TelephoneType.VIDEO)) {
                if (currentPhone.getText().length() > 0) {
                    return currentPhone.getText();
                }
            }
        }

        return null;
    }

    public String getRadioTelephoneNumber() {
        Iterator<Telephone> iter = (Iterator<Telephone>) this.vcard.getTelephoneNumbers().iterator();

        while (iter.hasNext()) {
            Telephone currentPhone = iter.next();
            if (currentPhone.getTypes().contains(TelephoneType.VOICE)) {
                if (currentPhone.getText().length() > 0) {
                    return currentPhone.getText();
                }
            }
        }

        return null;
    }

    public String getTTYTDDTelephoneNumber() {
        Iterator<Telephone> iter = (Iterator<Telephone>) this.vcard.getTelephoneNumbers().iterator();

        while (iter.hasNext()) {
            Telephone currentPhone = iter.next();
            if (currentPhone.getTypes().contains(TelephoneType.TEXT)) {
                if (currentPhone.getText().length() > 0) {
                    return currentPhone.getText();
                }
            }
        }

        return null;
    }

    public Date getLastModificationTime() {
        return this.dateLastModificationTme;
    }

    public String getPathToContactPicture() {
        return strPathToContactPicture;
    }

    public String getFileOnDavServer() {
        return strFileOnDavServer;
    }


}
