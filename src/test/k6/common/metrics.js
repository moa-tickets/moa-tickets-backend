// src/test/k6/common/metrics.js
import { Trend, Rate, Counter } from "k6/metrics";

// Hold 관련 메트릭
export const holdTrend = new Trend("booking_hold_duration");
export const holdFail = new Rate("booking_hold_fail_rate");
export const holdConflict = new Rate("booking_hold_conflict_409");
export const holdExpired = new Rate("booking_hold_expired_410");
export const holdAuth = new Rate("booking_hold_auth_401_403");
export const holdNotFound = new Rate("booking_hold_notfound_404");
export const holdServerErr = new Rate("booking_hold_5xx");
export const holdOk = new Counter("booking_hold_ok");
export const holdRetries = new Trend("booking_hold_retries");
export const holdTotalTrend = new Trend("booking_hold_total_duration", true);

// Payment 관련 메트릭
export const prepareTrend = new Trend("booking_prepare_duration");
export const confirmTrend = new Trend("booking_confirm_duration");
export const prepareFail = new Rate("booking_prepare_fail_rate");
export const confirmFail = new Rate("booking_confirm_fail_rate");
export const prepareOk = new Counter("booking_prepare_ok");
export const confirmOk = new Counter("booking_confirm_ok");
export const paymentTimeout = new Rate("payment_toss_timeout");

// E2E 메트릭
export const endToEndDuration = new Trend("booking_e2e_duration");