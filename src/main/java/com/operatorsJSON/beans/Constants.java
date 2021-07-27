package com.operatorsJSON.beans;

import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
public class Constants {
    public static final String HASHKEY = "cff44c52-d40e-46a4-acce-f6af7491529b";
    public static final String[] DEFAULTPASSWORDS = {"CgfyYLXu6sNF24nG", "vLBuNg5Sz7MAYCa9", "4pgbXMYLyRTE92NG", "sZqHkmLeNX64uhtV",
            "nLbvDKEyY5MuQCAq", "v2nW7EwDZ8hRPxLF", "74bMvw6NQfyV5jgY", "X4hMaJZUk3mnQy8H", "SZp5tC7fAgqUQYyj", "RDkKYfupZ97dVLhU",
            "TGALY46ZBk3nsam7", "cGfj35ge9DUtuxQ8", "FMcfTa6sb8nh4DH5", "DTEPUhJQZ3bAgHSN", "wma867rzeDhXdKtV", "zcRgkhCeHn4GDasU",
            "ZcTM9d24PmjFCtGU", "vw3RCNrygEkXf5SK", "NsxmwM6ZgrSfcLyk", "mU5LVChME2qfrknW", "2p45ChQEMHkGFWcn", "wzgFjKbv2ue6fskX",
            "csQPCBu2k3azjmfb", "PE7hnKJBp6D5fNHt", "pyrsxYEkcqd2XRtB", "rqCkLUzsjaxuW78X", "rkshYgc9NJ65qMTn", "zxEkKAWYFbs453vJ",
            "RwSuBXDTsYtM4KAq", "DXP4VCU79arSbf3L", "tNqLHuKncvM9JWT6", "ahPVmSjCtnuG5frq", "auTp86mbtNA4fWZR", "XUwvJmqN8BEDnuTR",
            "zj7KuvTUDmJYxC9B", "GtVCE6QRhzycDUSk", "gAMzHW7PkbG4NCU6", "8mGTQfdHuZaRX5L4", "7DPLdZCxGnWM9ShU", "LF3S9Rk5f2eBAMvX", "VAU4F67CasNczTuY",
            "QcCM3qs5UZrpGHdz", "8JnYgcZ4XtC5FDGH", "te3muHa6DZvFVpCS", "pExgrXeqzZ4F67dN", "NsqbXeBuF5gCLKAP", "ZWd8tvfJ7wFAqTEg", "bCRfSF3ayz7crLeT",
            "BDhZHymsVJ8rzA3T", "vaUPd5hMWmStXscg", "gcKSvMh95qHurBwJ", "RzQDSFy8nh7MBvw2", "pRhPEtBwzJTyCSG8", "r9dvGmCuK7QFP56S", "4crUTLeavdG2j9un",
            "wtGmjvMuB9dLD2Vp", "mj4wNWcUvkEMJnTg", "uR7sHjDya36JvBmp", "Qt8GcTAKdEmx9fDz", "QVaP68YZsCnkdSDv", "QuBUM4shNA6wam5T", "4J8bhnHVNjAPKBut",
            "WdmcLsR7f3PeQqxg", "UDChdjMxVcqmHE2Y", "mnWq7UjxPBJspKCt", "jkHaJqmR5hVNEPxf", "bEqLrkadGXVSy7w6", "9HEJ6dDjXfyz5GRu", "Cv85Vcf4sxQjSEJN",
            "7WTxrmqKQE8kHyZF", "ZU9Td3GbR7Wa6vx8", "AGR3WHmkyawdfqVj", "mpvt2TX9gPWCHedZ", "UJxKf8SBGjmFZs5b", "6Ayvr25ZRqBE7jTY", "2AxfgVyqb93KwRsD",
            "85w74fmtdZGzDqeJ", "HcA52XkEsCTtRadJ", "ChrbXjn5FPWDHwe7", "9tFyuZLVdWgcHPxf", "d7gasNyqWQk5h9xz", "27TpFcuDbnBEJth4", "aX59tGHEZrhAK7LS",
            "WN4tkqAhaDLu2QEj", "m4JRfM6n7vsUEQbz", "rj9f8QTm7RdCMtHL", "eEjPNh35S6w4dxXK", "cMTr4EpWP7NufG8n", "uwAP7dqsHZN68Cpj", "QAwtuahrdMjyKZq2",
            "n6pefSW2xXCmyaJ7", "qBE9VrZmcCnWdef2", "SZL4uD3KYaFThkvc", "w9kKjbJFSn52H4BX", "WXNeFwnLTJxqs2py", "UxP5QMCYved9tjRa", "UFquDJmvkKp58H4N",
            "PKRcEZnfLh6bH7VM", "UjKwnXGmQ3qu9cZe", "CqWNTHFjkEhY9DfL", "CJwAqT3Q2RWXsFDy", "vkYX9LwbhuDgf3jq", "Trf7KdahyVAZEHF3", "LDE9wUG6shHekjSF",
            "ELhpYUM7fBVcRxnS", "Tcu6axBwknQtvLz4", "A5XRMzwWxsbNhHEu", "L9GrSyFbVDgzxh8B", "82PH3nCpSGQRhdmT", "8YWZxvBhXK9ERrTA", "xhDRL7vc4FPjmSZH",
            "jwXTG5A8skBxQYCn", "5WqMeK4DXpBTmwhL", "L9tzrhucqPZpKkmF", "8bEvmK3QAu49LDVs", "pxN4myRVsfFc5UDw", "befmDJnaG9x26Z7B", "EUd34Y5JgpkP2WBf",
            "JHYDXGzNT7WuPdQj", "RA5rwcZeWFsMSQnz", "Vyk5g4u29njt7AXb", "nD5bkRHYGxLus2cq", "RxhLjZkKazE4w2mc", "8nj6YPzKFTtDCQLN", "U4PYSt8uqw5gGvRe",
            "tSuaXAFvdhCTM8UY", "Fu6PbJKcnkqhVYzH", "Ltp5jgy4ZQ9KGVX7", "68xHCSMXsD4mvufT", "MkR2TL8ASaWsBcXb", "6RULwHQGCgZdfxrt", "d3YBACqvrw5RbN9s",
            "WnC4UMHesjKGXv5c", "QDtkfpnjLNa8YxVH", "9jrdTaxyL4JYgnbB", "mu27WUCDKg6cVpTE", "9g2rMjJvhwtsXYRV", "XwW5Cujd9ZxRNJQr", "KG2DrYWeQ3LTSqzp",
            "6aT4pcVFkLERhb3t", "Bxm26jaUtQEdbzuL", "zBZuKqQD8PamWCV6", "CdpXnfEV4rUwgAZL", "kfLBdP42SGATjhZx", "KcnfEsvayPAwpzQ3", "AdKM7ZWXeUTvR3aE",
            "evjYbkhmTBy4HVW6", "LKQfj9mCPnvYbp6h", "pPStegYK2A9B5CjT", "6WKd43zUmVJs8v9B", "mVpEwKQGX9NP8D6x", "MY8JpkwHN4L97XWb", "4tgya6YVKJrqp3US",
            "Xg3eVYBNdWnFDSzA", "VJ3ZbMhmSUHfNdD6", "GuHAb2pdJwr7YSD4", "4AtSaTY87qMC2xmG", "RpDEhr2bsAUFecCn", "JEWK5TF2nUw76Vx4", "wrchW8N9QUfAxYp3",
            "4PWSZG8MVxmtsQXf", "eqX5ctFsnzfxwjgC", "DVNqavs6w39rdxLf", "furQtP96LZesHdwn", "8Ux6g3vKzZETHrVy", "nSxBML7bw4CYrqcA", "qGxLFa5dfhRPBZUm",
            "NxgVh7efDrWG5BJK", "xTuQbfXAgesVNkB7", "qxYBu6FsQhzVjXf5", "ftukKHcUZbaqgyR6", "yHSXsZLJrKnVf8pU", "N6cMHfPaB4sFAejh", "zCeB8V6XtZs2PG7h",
            "gbFUNQ23WADjTPsV", "X8ShuZtazK5EGkNF", "9dFb8XsALpB3PSuz", "pECG8SDgQzTaBhxb", "LgQKszEkx4v7tjHD", "5uYLRrTdBMyaQnNU", "6rvqWG2ayTEAs3uh",
            "NLxEWgAPju5DBdq2", "4Fmdgc9pXTaM5Nn2", "QFAJpBavK7rRYxbg", "NnVW9GPTeXRFg83u", "NcCH6kBGaFhAuEMs", "ftjPpMgNmvQWGw8q", "QhreJaXKF57R6YPg",
            "ztdA8Gfn7MSFLRQe", "MfK4bgBGnqr59JzV", "BE7ps9eZkuvYKJz3", "czZqfyW9VNkKgwsD", "pTXejR2g63hDKUfL", "2guMyLraQJDseZSN", "djxfXQVaPUgMZFD2",
            "REY9vtC5A3dpSnac", "Gqf3r9p4eczMk8wC", "XcPHZ3QxzqRwsmtr", "KuAq4jcv7Ux5zYBS", "WhemfsUy75S26wKQ", "h9PL4AKMEeFdUwSr", "EjmdPnT4p3kRHZ95",
            "bmUkdYaZEpRn5H3g", "j42XuSAnRyvPkLFC", "7rAFcnpwLC5qMKBf", "FCGbY25WQzVNkSmc", "RvrpHg7dajxy2DzU", "HSXrpPwGtzmWKZ9c", "WVy94DZamTdBF5Ck",
            "ZUEJWvC6fprnDgPz", "aG6sV2kA9gLTUEhz", "u9fFPR4JUs3kSjNM", "gWavHzTUA6ZQkRsD", "zLvXVRZe7MCr2dwT", "LcdjzrQq3gAXYRZh", "4VKwCvcNqp3mBEJ6",
            "sDFvpG4kcPmnL7Uw", "tD6u9Z5aKkVePvdW", "5wM9Un2VXbaAdCEe", "5LtCW6R4VXZ3EBnw", "Sy4rKpEDRnbWVPv3", "GehBULXRtJ7pwAsb", "kAdz675PwXcZEBuK",
            "T8S7pqwUrXN6bQ4e", "tDuJ5vNjsFUaQWEc", "GSvpDWKNmALF28cT", "38GvtFhPpJsrbVL6", "3PzWfuRmBwSrJFaQ", "PwKSvjRMVGUXhq8L", "VuLTEH48nGWySg2c",
            "Z6w4fz58hCcpBA9M", "fcjpEmD8dRa2GghB", "yNkKcetXUvF6Mqum", "CgQc947YR6qZE3zu", "RexcvnqHC2MmhA76", "nBZyfWsCTRpr6xqu", "fcyauAsBXGH6j29e",
            "bexmrNU4SgEjJRLv", "r4uBUea28WPG5kgz", "EmcxRHhM7yLQA8st", "59qXK7crTDvGVBps", "zuTsygDd8FU2WNLE", "2vV4sMkWwCgzGdBF", "hcrPzL6QbskBDGNY",
            "5FY3LaNej4SXmhZd", "KenPVxBzN3SZCG6W", "NZbk2XRHawfEgVvK", "5gHFtnMwXJNedR4v", "brD7RBgCHMnELc5A", "5bsChcZAaTPq8Rx6"
    };
//    public static HashMap<String, String> EXPECTEDRESPONSES = (HashMap<String, String>) Map.of(
//            "Case_0", "\"{\n" +
//                    "  \"\"operatorId\"\" : 13000000,\n" +
//                    "  \"\"uid\"\" : \"\"57137\"\",\n" +
//                    "  \"\"nickName\"\" : \"\"Martin Luther King\"\",\n" +
//                    "  \"\"playerTokenAtLaunch\"\" : \"\"sfsdfsdfsdf\"\",\n" +
//                    "  \"\"token\"\" : \"\"a1c1f9c192a79b10f0e7bca36af59ba9\"\",\n" +
//                    "  \"\"balance\"\" : 1000.0,\n" +
//                    "  \"\"currency\"\" : \"\"USD\"\",\n" +
//                    "  \"\"language\"\" : \"\"en\"\",\n" +
//                    "  \"\"date\"\" : \"\"2020-05-03 08:18:11.231288\"\",\n" +
//                    "  \"\"clientIP\"\" : \"\"127.0.0.1\"\",\n" +
//                    "  \"\"errorCode\"\" : 0,\n" +
//                    "  \"\"errorDescription\"\" : \"\"ok\"\",\n" +
//                    "  \"\"timestamp\"\" : 1588493891231,\n" +
//                    "  \"\"vip\"\" : \"\"3\"\"\n" +
//                    "}\"\n",
//            "Case_1", "\"{\n" +
//                    "\"\"operatorId\"\" : 13000000,\n" +
//                    "  \"\"balance\"\" : 0.0,\n" +
//                    "  \"\"errorCode\"\" : 6,\n" +
//                    "  \"\"errorDescription\"\" : \"\"Token not found.\"\",\n" +
//                    "  \"\"timestamp\"\" : 1588518141005\n" +
//                    "}\"\n",
//            "All_Cases", "\"{\n" +
//                    "  \"\"operatorId\"\" : 13000000,\n" +
//                    "  \"\"roundId\"\" : 45354,\n" +
//                    "  \"\"uid\"\" : \"\"57137\"\",\n" +
//                    "  \"\"nickName\"\" : \"\"Martin Luther King\"\",\n" +
//                    "  \"\"token\"\" : \"\"a1c1f9c192a79b10f0e7bca36af59ba9\"\",\n" +
//                    "  \"\"balance\"\" : 999.0,\n" +
//                    "  \"\"transactionId\"\" : \"\"030fb5c8-b718-405f-88a9-28889eed46af\"\",\n" +
//                    "  \"\"currency\"\" : \"\"USD\"\",\n" +
//                    "  \"\"bonusAmount\"\" : 0.0,\n" +
//                    "  \"\"errorCode\"\" : 0,\n" +
//                    "  \"\"errorDescription\"\" : \"\"OK\"\",\n" +
//                    "  \"\"timestamp\"\" : 1588493901716\n" +
//                    "}\"\n");

    public static final String[] successfulAuthMandatoryKeys = {"operatorId", "uid", "token", "balance", "currency", "errorCode", "errorDescription", "timestamp"};
    public static final String[] unsuccessfulAuthMandatoryKeys = {"operatorId", "errorCode", "errorDescription", "timestamp"};
    public static final String[] mandatoryKeys = {"operatorId", "roundId", "uid", "token", "balance", "transactionId", "currency", "errorCode", "errorDescription", "timestamp"};
    public static final String[] optionalKeys = {"nickName", "playerTokenAtLaunch", "clientIP", "VIP", "bonusAmount"};

    public static String formatMyDouble(double num) {
        DecimalFormat decimalFormat = new DecimalFormat("#.00");
        return decimalFormat.format(num);
    }

    public enum ERRORSTATE {OK,W,E};
}
