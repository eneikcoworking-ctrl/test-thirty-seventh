package com.eneik.generated.repository;

import com.eneik.generated.domain.Proxy;
import com.eneik.generated.domain.TgAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;

@DataJpaTest
@ActiveProfiles("test")
public class TgAccountRepositoryTest {

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private TgAccountRepository tgAccountRepository;

    @Test
    public void testStoreProxyAndTgAccount() {
        Proxy proxy = new Proxy();
        proxy.setIpAddress("192.168.1.1");
        proxy.setPort(8080);
        proxy.setProtocol("SOCKS5");
        Proxy savedProxy = proxyRepository.save(proxy);

        TgAccount account = new TgAccount();
        account.setPhoneNumber("+1234567890");
        account.setStatus("Active");
        account.setProxy(savedProxy);
        TgAccount savedAccount = tgAccountRepository.save(account);

        Optional<TgAccount> retrievedAccount = tgAccountRepository.findById(savedAccount.getId());
        assertThat(retrievedAccount).isPresent();
        assertThat(retrievedAccount.get().getProxy()).isNotNull();
        assertThat(retrievedAccount.get().getProxy().getIpAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    public void testUpdateAccountStatusToPermanentBan() {
        TgAccount account = new TgAccount();
        account.setPhoneNumber("+0987654321");
        account.setStatus("Active");
        TgAccount savedAccount = tgAccountRepository.save(account);

        savedAccount.setStatus("Permanent Ban");
        tgAccountRepository.save(savedAccount);

        Optional<TgAccount> retrievedAccount = tgAccountRepository.findById(savedAccount.getId());
        assertThat(retrievedAccount).isPresent();
        assertThat(retrievedAccount.get().getStatus()).isEqualTo("Permanent Ban");
    }
}
