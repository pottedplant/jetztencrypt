package jetztencrypt.client;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;

public class RegistrationOptions {

	public final ImmutableList<String> contact;
	public final String agreement;

	public RegistrationOptions(ImmutableList<String> contact,String agreement) {
		this.contact = Verify.verifyNotNull(contact);
		this.agreement = agreement;
	}

	public static RegistrationOptions forContact(String...contact) {
		if( contact==null ) return new RegistrationOptions(ImmutableList.of(),null);
		return new RegistrationOptions(ImmutableList.copyOf(contact),null);
	}

	public RegistrationOptions withAgreement(String agreement) {
		return new RegistrationOptions(contact,agreement);
	}

}
