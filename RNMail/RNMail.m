#import <MessageUI/MessageUI.h>
#import "RNMail.h"
#import <React/RCTConvert.h>
#import <React/RCTLog.h>

@implementation RNMail
{
    NSMutableDictionary *_callbacks;
}

- (instancetype)init
{
    if ((self = [super init])) {
        _callbacks = [[NSMutableDictionary alloc] init];
    }
    return self;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

+ (BOOL)requiresMainQueueSetup
{
    return YES;
}

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(mail:(NSDictionary *)options
                  callback: (RCTResponseSenderBlock)callback)
{
    if ([MFMailComposeViewController canSendMail])
    {
        MFMailComposeViewController *mail = [[MFMailComposeViewController alloc] init];
        mail.mailComposeDelegate = self;
        _callbacks[RCTKeyForInstance(mail)] = callback;

        if (options[@"subject"]){
            NSString *subject = [RCTConvert NSString:options[@"subject"]];
            [mail setSubject:subject];
        }

        bool *isHTML = NO;

        if (options[@"isHTML"]){
            isHTML = [options[@"isHTML"] boolValue];
        }

        if (options[@"body"]){
            NSString *body = [RCTConvert NSString:options[@"body"]];
            [mail setMessageBody:body isHTML:isHTML];
        }

        if (options[@"recipients"]){
            NSArray *recipients = [RCTConvert NSArray:options[@"recipients"]];
            [mail setToRecipients:recipients];
        }

        if (options[@"ccRecipients"]){
            NSArray *ccRecipients = [RCTConvert NSArray:options[@"ccRecipients"]];
            [mail setCcRecipients:ccRecipients];
        }

        if (options[@"bccRecipients"]){
            NSArray *bccRecipients = [RCTConvert NSArray:options[@"bccRecipients"]];
            [mail setBccRecipients:bccRecipients];
        }

        if (options[@"attachmentList"] ){
            NSArray *attachments = [RCTConvert NSArray:options[@"attachmentList"]];

            for(NSDictionary *attachment in attachments){
                NSString *path = [RCTConvert NSString:attachment[@"path"]];
                NSString *type = [RCTConvert NSString:attachment[@"mimeType"]];
                NSString *name = [RCTConvert NSString:attachment[@"name"]];

                if (name == nil){
                    name = [[path lastPathComponent] stringByDeletingPathExtension];
                }

                if (type == nil){
                    type = [[path lastPathComponent] pathExtension];
                }
                // Get the resource path and read the file using NSData
                NSData *fileData = [NSData dataWithContentsOfFile:path];

                // Determine the MIME type
                NSString *mimeType;
                if (type != nil){
                    if ([type localizedCaseInsensitiveCompare:@"jpg"] == NSOrderedSame) {
                        mimeType = @"image/jpeg";
                    } else if ([type localizedCaseInsensitiveCompare:@"png"] == NSOrderedSame) {
                        mimeType = @"image/png";
                    } else if ([type localizedCaseInsensitiveCompare:@"doc"] == NSOrderedSame) {
                        mimeType = @"application/msword";
                    } else if ([type localizedCaseInsensitiveCompare:@"docx"] == NSOrderedSame) {
                        mimeType = @"application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    } else if ([type localizedCaseInsensitiveCompare:@"ppt"] == NSOrderedSame) {
                        mimeType = @"application/vnd.ms-powerpoint";
                    } else if ([type localizedCaseInsensitiveCompare:@"pptx"] == NSOrderedSame) {
                        mimeType = @"application/vnd.openxmlformats-officedocument.presentationml.presentation";
                    } else if ([type localizedCaseInsensitiveCompare:@"html"] == NSOrderedSame) {
                        mimeType = @"text/html";
                    } else if ([type localizedCaseInsensitiveCompare:@"csv"] == NSOrderedSame) {
                        mimeType = @"text/csv";
                    } else if ([type localizedCaseInsensitiveCompare:@"pdf"] == NSOrderedSame) {
                        mimeType = @"application/pdf";
                    } else if ([type localizedCaseInsensitiveCompare:@"vcard"] == NSOrderedSame) {
                        mimeType = @"text/vcard";
                    } else if ([type localizedCaseInsensitiveCompare:@"json"] == NSOrderedSame) {
                        mimeType = @"application/json";
                    } else if ([type localizedCaseInsensitiveCompare:@"zip"] == NSOrderedSame) {
                        mimeType = @"application/zip";
                    } else if ([type localizedCaseInsensitiveCompare:@"text"] == NSOrderedSame) {
                        mimeType = @"text/*";
                    } else if ([type localizedCaseInsensitiveCompare:@"mp3"] == NSOrderedSame) {
                        mimeType = @"audio/mpeg";
                    } else if ([type localizedCaseInsensitiveCompare:@"wav"] == NSOrderedSame) {
                        mimeType = @"audio/wav";
                    } else if ([type localizedCaseInsensitiveCompare:@"aiff"] == NSOrderedSame) {
                        mimeType = @"audio/aiff";
                    } else if ([type localizedCaseInsensitiveCompare:@"flac"] == NSOrderedSame) {
                        mimeType = @"audio/flac";
                    } else if ([type localizedCaseInsensitiveCompare:@"ogg"] == NSOrderedSame) {
                        mimeType = @"audio/ogg";
                    } else if ([type localizedCaseInsensitiveCompare:@"xls"] == NSOrderedSame) {
                        mimeType = @"application/vnd.ms-excel";
                    } else if ([type localizedCaseInsensitiveCompare:@"xlsx"] == NSOrderedSame) {
                        mimeType = @"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    } else if ([type localizedCaseInsensitiveCompare:@"mov"] == NSOrderedSame) {
                        mimeType = @"video/quicktime";
                    } else if ([type localizedCaseInsensitiveCompare:@"mp4"] == NSOrderedSame) {
                        mimeType = @"video/mp4"
                    }

                    if(mimeType != nil && name != nil && fileData != nil){
                        [mail addAttachmentData:fileData mimeType:mimeType fileName:name];
                    }
                }
                
            }
        }

        UIViewController *root = [[[[UIApplication sharedApplication] delegate] window] rootViewController];

        while (root.presentedViewController) {
            root = root.presentedViewController;
        }
        [root presentViewController:mail animated:YES completion:nil];
    } else {
        callback(@[@"not_available"]);
    }
}

#pragma mark MFMailComposeViewControllerDelegate Methods

- (void)mailComposeController:(MFMailComposeViewController *)controller didFinishWithResult:(MFMailComposeResult)result error:(NSError *)error
{
    NSString *key = RCTKeyForInstance(controller);
    RCTResponseSenderBlock callback = _callbacks[key];
    if (callback) {
        switch (result) {
            case MFMailComposeResultSent:
                callback(@[[NSNull null] , @"sent"]);
                break;
            case MFMailComposeResultSaved:
                callback(@[[NSNull null] , @"saved"]);
                break;
            case MFMailComposeResultCancelled:
                callback(@[[NSNull null] , @"cancelled"]);
                break;
            case MFMailComposeResultFailed:
                callback(@[@"failed"]);
                break;
            default:
                callback(@[@"error"]);
                break;
        }
        [_callbacks removeObjectForKey:key];
    } else {
        RCTLogWarn(@"No callback registered for mail: %@", controller.title);
    }
    UIViewController *ctrl = [[[[UIApplication sharedApplication] delegate] window] rootViewController];
    while (ctrl.presentedViewController && ctrl != controller) {
        ctrl = ctrl.presentedViewController;
    }
    [ctrl dismissViewControllerAnimated:YES completion:nil];
}

#pragma mark Private

static NSString *RCTKeyForInstance(id instance)
{
    return [NSString stringWithFormat:@"%p", instance];
}

@end
