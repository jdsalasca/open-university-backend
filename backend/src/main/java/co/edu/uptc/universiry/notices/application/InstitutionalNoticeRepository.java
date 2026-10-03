package co.edu.uptc.universiry.notices.application;

import co.edu.uptc.universiry.notices.domain.InstitutionalNotice;

import java.util.List;

public interface InstitutionalNoticeRepository {

    void append(InstitutionalNotice notice);

    List<InstitutionalNotice> recent(int limit);
}