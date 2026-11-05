package ru.kvizcap.quiz.app.startup

import org.junit.Assert.assertEquals
import org.junit.Test

class NonOrganicLinkTest {
    @Test fun appIdAndCampaignPartsAreAddedInOrder() {
        assertEquals("https://example.com?app_id=af-123&sub1=one&sub2=two&sub3=three",
            nonOrganicLink("https://example.com", "af-123", "one_two_three"))
    }

    @Test fun originalQueryAndFragmentArePreservedAndOldAttributionIsReplaced() {
        assertEquals("HTTPS://EXAMPLE.com/a%2Fb?keep=a+b&next=x%2Fy&app_id=new&sub1=first&sub2=second#Page",
            nonOrganicLink("HTTPS://EXAMPLE.com/a%2Fb?keep=a+b&app_id=old&sub1=old&sub9=stale&next=x%2Fy#Page", "new", "first_second"))
    }

    @Test fun missingOrEmptyCampaignAddsOnlyAppId() {
        for (campaign in listOf(null, "")) {
            assertEquals("https://example.com?keep=1&app_id=uid",
                nonOrganicLink("https://example.com?keep=1&sub1=old", "uid", campaign))
        }
    }

    @Test fun emptyCampaignPartsKeepTheirPositionsIncludingTrailingPart() {
        assertEquals("https://example.com?app_id=id&sub1=&sub2=one&sub3=&sub4=three&sub5=",
            nonOrganicLink("https://example.com", "id", "_one__three_"))
    }

    @Test fun queryValuesAreEncodedWithoutChangingTheBase() {
        assertEquals("https://example.com/a%2Fb?app_id=id%26x%3D1&sub1=a%20b&sub2=x%26y&sub3=%D1%82%D0%B5%D1%81%D1%82",
            nonOrganicLink("https://example.com/a%2Fb", "id&x=1", "a b_x&y_тест"))
    }

    @Test fun arbitraryFirebaseTextAndBareDomainsRemainAccepted() {
        for (base in listOf("12ук", "google.com", " ")) {
            assertEquals("$base?app_id=id&sub1=a&sub2=b", nonOrganicLink(base, "id", "a_b"))
        }
    }

    @Test fun existingQueryPlaceholdersAreReplacedWithoutDuplicateParameters() {
        assertEquals("https://example.com?fixed=1&app_id=id&sub1=a&sub2=b#anchor",
            nonOrganicLink("https://example.com?app_id={app_id}&sub1={sub1}&fixed=1#anchor", "id", "a_b"))
    }

    @Test fun metaStyleTemplateFillsAllSixFieldsAndAppendsCampaignParts() {
        val template = "https://example.com/click?key=fixed&ad_id={{ad.id}}&adset_id={{adset.id}}&campaign_id={{campaign.id}}&ad_name={{ad.name}}&adset_name={{adset.name}}&campaign_name={{campaign.name}}"
        val data = mapOf("ad_id" to "111", "adset_id" to "222", "campaign_id" to "333",
            "adgroup" to "Video A&B", "adset" to "Аудитория 1", "campaign" to "one_two")
        assertEquals("https://example.com/click?key=fixed&ad_id=111&adset_id=222&campaign_id=333&ad_name=Video%20A%26B&adset_name=%D0%90%D1%83%D0%B4%D0%B8%D1%82%D0%BE%D1%80%D0%B8%D1%8F%201&campaign_name=one_two&app_id=uid&sub1=one&sub2=two",
            nonOrganicLink(template, "uid", "one_two", data))
    }

    @Test fun afPrefixedAndCustomFieldsFillPlainAndEncodedMacros() {
        val data = mapOf("af_ad_id" to "ad", "af_adset_id" to "set", "af_c_id" to "campaign-id",
            "af_ad" to "Creative", "af_adset" to "Group", "custom" to "x/y+z%")
        assertEquals("https://example.com?ad=ad&set=set&campaign=campaign-id&name=Creative&group=Group&extra=x%2Fy%2Bz%25&uid=uid&app_id=uid&sub1=a&sub2=b",
            nonOrganicLink("https://example.com?ad=%7B%7Bad.id%7D%7D&set={{adset.id}}&campaign={campaign.id}&name={{ad.name}}&group={{adset.name}}&extra={{custom}}&uid={appsflyer_id}", "uid", "a_b", data))
    }

    @Test fun missingOrEmptyValuesLeaveTheirMacrosExactlyAsProvided() {
        assertEquals("https://example.com?ad={{ad.id}}&name=%7b%7bad.name%7d%7d&other={{unknown}}&app_id=uid&sub1=one",
            nonOrganicLink("https://example.com?ad={{ad.id}}&name=%7b%7bad.name%7d%7d&other={{unknown}}", "uid", "one", mapOf("ad_id" to "", "af_ad" to "")))
    }

    @Test fun literalValuesAndUnrelatedEscapingStayUnchangedAndValuesAreNotExpandedRecursively() {
        assertEquals("HTTPS://EXAMPLE.com/a%2Fb?key=a+b&ad=already%2Ffilled&custom=%7B%7Bad.id%7D%7D&app_id=id#Keep",
            nonOrganicLink("HTTPS://EXAMPLE.com/a%2Fb?key=a+b&ad=already%2Ffilled&custom={{custom}}#Keep", "id", null,
                mapOf("custom" to "{{ad.id}}", "ad_id" to "must-not-expand")))
    }

    @Test fun oldAndNewSubKeysAreReplacedWithoutDuplicates() {
        assertEquals("12ук?keep=1&app_id=id&sub1=a&sub2=&sub3=b&sub4=",
            nonOrganicLink("12ук?sub_1=old&sub1=old&sub99=stale&sub_99=stale&%61pp_id=old&keep=1", "id", "a__b_"))
    }

    @Test fun directMacroNamesTakePriorityAndCampaignCanComeFromThePayload() {
        assertEquals("https://example.com?ad=direct&campaign=one_two&app_id=uid&sub1=one&sub2=two",
            nonOrganicLink("https://example.com?ad={{ad.id}}&campaign={{campaign.name}}", "uid", null,
                mapOf("ad.id" to "direct", "ad_id" to "fallback", "campaign_name" to "one_two")))
    }
}
