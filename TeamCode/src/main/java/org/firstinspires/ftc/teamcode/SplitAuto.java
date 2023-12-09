package org.firstinspires.ftc.teamcode;

import static java.lang.Math.toRadians;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;

import autoThings.roadRunner.drive.SampleMecanumDrive;
import autoThings.roadRunner.trajectorysequence.TrajectorySequence;

@Autonomous
public class SplitAuto extends OpMode {
    Board board = new Board();

    int spike = 0;
    SampleMecanumDrive drive;
    TrajectorySequence sequence1, sequence2, sequence3;

    final double DESIRED_DISTANCE = 8; // inches
    private AprilTagDetection desiredTag = null;
    AprilTagProcessor aprilTag;
    final int DESIRED_TAG_ID = -1;
    final double speedGain = 0.02;
    final double strafeGain = 0.015;
    final double turnGain = 0.01;
    final double maxAutoSpeed = 0.5;
    final double maxAutoStrafe = 0.5;
    final double maxAutoTurn = 0.3;
    boolean Harvey;

    boolean targetFound = false;
    double  driveAprilTag = 0;
    double  strafeAprilTag = 0;
    double  turnAprilTag = 0;

    @Override
    public void init() {
        drive = new SampleMecanumDrive(hardwareMap);
        drive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        try {
            board.getHW(hardwareMap, telemetry, true);
        } catch (Throwable e) {
            telemetry.addData("Could not access hardware because ", e);
        }
        try {
            board.changeToPos();
        } catch (Throwable e) {
            telemetry.addData("Trouble with accessing wheels because ", e);
        }
        drive.setPoseEstimate(new Pose2d(-36.0, 61.0, toRadians(270.0)));

        sequence1 = drive.trajectorySequenceBuilder(new Pose2d(-36.0, 61.0, toRadians(270.0)))
                .setVelConstraint(SampleMecanumDrive.getVelocityConstraint(
                        40,
                        1.5,
                        14.97))
                .lineToConstantHeading(new Vector2d(-36.0, 35.0))
                .addDisplacementMarker(() -> {
                    // TODO figure out how to make this work
                    try {
                        if (board.getEyes().getTfod().getRecognitions().size() != 0) {
                            spike = 1;
                            board.setIntake(-1);
                            wait(1000);
                            board.setIntake(0);
                        }
                    } catch (Throwable e) {
                        telemetry.addData("Could not see because", e);
                    } finally {
                        drive.turn(90);
                        try {
                            if (board.getEyes().getTfod().getRecognitions().size() != 0) {
                                spike = 2;
                                board.setIntake(-1);
                                wait(1000);
                                board.setIntake(0);
                            }
                        } catch (Throwable e) {
                            telemetry.addData("Could not see because", e);
                        } finally {
                            drive.turn(180 - 1e-6);
                            try {
                                if (board.getEyes().getTfod().getRecognitions().size() != 0) {
                                    spike = 3;
                                    board.setIntake(-1);
                                    wait(1000);
                                    board.setIntake(0);
                                }
                            } catch (Throwable e) {
                                telemetry.addData("Could not see because", e);
                            } finally {
                                drive.turn(90);
                                drive.followTrajectorySequenceAsync(sequence2);
                            }
                        }
                    }
                })
                .build();

        sequence2 = drive.trajectorySequenceBuilder(sequence1.end())
                .addDisplacementMarker(() -> drive.updatePoseEstimate())
                .lineToConstantHeading(new Vector2d(-36.0, 40.0))
                .splineToConstantHeading(new Vector2d(-53.0, 50.0), toRadians(135.0))
                .lineToConstantHeading(new Vector2d(-53.0, 12.0))
                .splineToConstantHeading(new Vector2d(-20.0, 0.0), toRadians(270.0))
                .lineToConstantHeading(new Vector2d(20.0, 0.0))
                .addDisplacementMarker(() -> {
                    //TODO what was this for? I need to figure that out
                })
                .splineToSplineHeading(new Pose2d(44.0, 30.0, 0.0), 0.0)
                .addDisplacementMarker(() -> {
                    Harvey = true;
                })
                .build();

        sequence3 = drive.trajectorySequenceBuilder(new Pose2d())
                .splineToSplineHeading(new Pose2d(49.0, 30.0, 0.0), 0.0)
                .addDisplacementMarker(() -> {
                    board.setSlideTar(1000); //TODO find the real value
                })
                .lineToConstantHeading(new Vector2d(45.0, 25.0))
                .splineToConstantHeading(new Vector2d(60.0, 12.0), toRadians(10.0))
                .build();

        telemetry.addLine("compiled");
        telemetry.update();
        drive.followTrajectorySequenceAsync(sequence1);
    }

    @Override
    public void init_loop() {
        try { //start of TensorFlow
            board.getEyes().getTfod().getRecognitions().forEach(thing -> telemetry.addLine("found " + thing));
        } catch (Throwable e) {
            telemetry.addData("Error in using camera because:", e);
        } //end of tensorFlow

        try { //start of April tags
            if (board.getEyes().getApril().getDetections().size() > 0) {
                AprilTagDetection tag = board.getEyes().getApril().getDetections().get(0);

                telemetry.addData("x", tag.ftcPose.x);
                telemetry.addData("y", tag.ftcPose.y);
                telemetry.addData("z", tag.ftcPose.z);
                telemetry.addData("roll", tag.ftcPose.roll);
                telemetry.addData("pitch", tag.ftcPose.pitch);
                telemetry.addData("yaw", tag.ftcPose.yaw);
            }
        } catch (Throwable e) {
            telemetry.addData("Issue with April Tags because ", e);
        } // end of April Tags
    }

    @Override
    public void loop() {
        targetFound = false;
        desiredTag  = null;

        drive.update();
        // april tag code here

        List<AprilTagDetection> currentDetections = aprilTag.getDetections();
        for (AprilTagDetection detection : currentDetections) {
            // Look to see if we have size info on this tag.
            if (detection.metadata != null) {
                //  Check to see if we want to track towards this tag.
                if ((DESIRED_TAG_ID < 0) || (detection.id == DESIRED_TAG_ID)) {
                    // Yes, we want to use this tag.
                    targetFound = true;
                    desiredTag = detection;
                    break;
                }
            }
        }

        if(targetFound && Harvey) {
            // calculate range, heading, yaw error to figure out what the speed of the bot should be
            double rangeError = (desiredTag.ftcPose.range - DESIRED_DISTANCE);;
            double headError = desiredTag.ftcPose.bearing - 0;;
            double yawError = desiredTag.ftcPose.yaw - 0;

            driveAprilTag  = Range.clip(rangeError * speedGain , -maxAutoSpeed, maxAutoSpeed);
            turnAprilTag   = Range.clip(headError * turnGain, -maxAutoTurn, maxAutoTurn) ;
            strafeAprilTag = Range.clip(-yawError * strafeGain, -maxAutoStrafe, maxAutoStrafe);

//            board.drive(driveAprilTag, strafeAprilTag, turnAprilTag);
            drive.setDrivePower(new Pose2d(driveAprilTag, strafeAprilTag, turnAprilTag));
            //april tag done
        }
        // TODO sequence 3 road runner here

    }
}